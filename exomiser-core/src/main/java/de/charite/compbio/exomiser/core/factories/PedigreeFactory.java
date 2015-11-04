
/*
 * The Exomiser - A tool to annotate and prioritize variants
 *
 * Copyright (C) 2012 - 2015  Charite Universitätsmedizin Berlin and Genome Research Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.charite.compbio.exomiser.core.factories;

import de.charite.compbio.exomiser.core.model.SampleData;
import de.charite.compbio.jannovar.pedigree.Disease;
import de.charite.compbio.jannovar.pedigree.PedFileContents;
import de.charite.compbio.jannovar.pedigree.PedFileReader;
import de.charite.compbio.jannovar.pedigree.PedParseException;
import de.charite.compbio.jannovar.pedigree.PedPerson;
import de.charite.compbio.jannovar.pedigree.Pedigree;
import de.charite.compbio.jannovar.pedigree.PedigreeExtractor;
import de.charite.compbio.jannovar.pedigree.Person;
import de.charite.compbio.jannovar.pedigree.Sex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

/**
 * Handles the creation of Pedigree objects.
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class PedigreeFactory {

    private static final Logger logger = LoggerFactory.getLogger(PedigreeFactory.class);

    public static final String DEFAULT_SAMPLE_NAME = "unnamed sample";

    /**
     * The Exomiser can perform filtering of VCF data according to pedigree
     * data. If the VCF file contains multiple samples, then the Exomiser
     * requires a corresponding PED file to be able to analyze the inheritance
     * pattern of the variants. The PED file is parsed by the PEDFileParser
     * class in jannovar. This results in a Pedigree object that is used to do
     * the pedigree analysis. The VCF Parser from Jannovar creates Genotype
     * objects for each variant in the VCF file, and these can be compared to
     * the pedigree information. The {@link de.charite.compbio.exomiser.core.model.Gene Gene} class
     * coordinates this analysis.
     * <P>
     * Note that for single-sample VCF files, a Pedigree object is still
     * constructed, and we assume that the sample is from an affected person.
     *
     * @param pedigreeFilePath
     * @param sampleData
     * @return
     */
    public Pedigree createPedigreeForSampleData(Path pedigreeFilePath, SampleData sampleData) {
        List<String> sampleNames = createSampleNames(sampleData);
        int numberOfSamples = sampleData.getNumberOfSamples();
        logger.info("Creating pedigree for VCF containing {} sample(s)", numberOfSamples);
        switch (numberOfSamples) {
            case 0:
                throw new PedigreeCreationException("No data present in sampleData");
            case 1:
                return createSingleSamplePedigree(sampleNames);
            default:
                return createMultiSamplePedigree(pedigreeFilePath, sampleNames);
        }
    }

    private List<String> createSampleNames(SampleData sampleData) {
        List<String> sampleDataNames = sampleData.getSampleNames();
        return new ArrayList<>(sampleDataNames);
    }

    private Pedigree createSingleSamplePedigree(List<String> sampleNames) {
        String sampleName = DEFAULT_SAMPLE_NAME;
        if (!sampleNames.isEmpty()) {
            sampleName = sampleNames.get(0);
        }

        final Person person = new Person(sampleName, null, null, Sex.UNKNOWN, Disease.AFFECTED);
        return new Pedigree("family", ImmutableList.of(person));
    }

    private Pedigree createMultiSamplePedigree(Path pedigreeFilePath, List<String> sampleNames) {
        logger.info("Reading pedigree file: {}", pedigreeFilePath);
        final PedFileContents pedFileContents = checkAndBuildPedFileContents(pedigreeFilePath);
        logger.info("Pedigree {} contains {} individuals", pedFileContents.getIndividuals().get(0).getPedigree(), pedFileContents.getIndividuals().size());

        final Pedigree pedigree = checkNamesAndBuildPedigree(sampleNames, pedFileContents);
        logger.info("Created pedigree for family {} with members {}", pedigree.getName(), pedigree.getNames());
        return pedigree;
    }

    private PedFileContents checkAndBuildPedFileContents(Path pedigreeFilePath) {
        final PedFileContents pedFileContents = readPedFileContents(pedigreeFilePath);
        checkPedFileContentsContainsSingleFamily(pedFileContents);
        return pedFileContents;
    }

    private PedFileContents readPedFileContents(Path pedigreeFilePath) {
        checkPedigreePathIsNotNull(pedigreeFilePath);
        try {
            PedFileContents pedFileContents = new PedFileReader(pedigreeFilePath.toFile()).read();
            if (pedFileContents.getIndividuals().isEmpty()) {
                //might never happen, but anyway...
                throw new PedigreeCreationException("PED file contains no valid individuals - Check PED file format.");
            }
            return pedFileContents;

        } catch (PedParseException e) {
            throw new PedigreeCreationException("Problem parsing the PED file - Check PED file format, fields should be TAB separated.", e);
        } catch (IOException e) {
            throw new PedigreeCreationException("Problem reading the PED file", e);
        }
    }

    private void checkPedigreePathIsNotNull(Path pedigreeFilePath) throws PedigreeCreationException {
        if (pedigreeFilePath == null) {
            logger.error("PED file must be be provided for multi-sample VCF files.");
            //terminate the program - we really need one of these.
            throw new PedigreeCreationException("Pedigree file path cannot be null.", new NullPointerException());
        }
    }

    private void checkPedFileContentsContainsSingleFamily(PedFileContents pedFileContents) throws PedigreeCreationException {
        List<String> familyNames = pedFileContents.getIndividuals().stream().map(PedPerson::getPedigree).distinct().collect(toList());
        if (familyNames.size() > 1) {
            throw new PedigreeCreationException("PED file must contain only one family, found " + familyNames.size() + ": " + familyNames + ". Please provide PED file containing only the proband family matching supplied HPO terms.");
        }
    }

    private Pedigree checkNamesAndBuildPedigree(List<String> sampleNames, PedFileContents pedFileContents) {
        //This step is important - we want a catastrophic failure here is the PED and VCF file sample names don't match in case the inheritance mode analysis goes wrong later.
        //If we can be assured that this isn't a problem fro Jannovar to check this method can be removed.
        checkPedFileContentsMatchesSampleNames(pedFileContents, sampleNames);

        return buildPedigree(pedFileContents);
    }


    private void checkPedFileContentsMatchesSampleNames(PedFileContents pedFileContents, List<String> sampleNames) throws PedigreeCreationException {
        logger.info("Sample names from VCF: {}", sampleNames);
        logger.info("Individuals from PED:  {}", pedFileContents.getNameToPerson().keySet());
        logger.info("Matching VCF sample names with PED individuals...");
        //yes, we could just do a set comparison here, but we want to throw an exception once we've logged all the unrepresented individuals for the user.
        Map<Boolean, List<PedPerson>> people = pedFileContents.getIndividuals().stream().collect(partitioningBy(pedPerson -> sampleNames.contains(pedPerson.getName())));

        List<PedPerson> representedPeople = people.get(true);
        if (representedPeople.isEmpty()) {
            throw new PedigreeCreationException("VCF - PED mismatch. None of the individuals in the PED match any of the sample names in the VCF");
        }

        List<PedPerson> unrepresentedPeople = people.get(false);
        if (unrepresentedPeople.size() > 0) {
            unrepresentedPeople.forEach(
                    person -> logger.error("Individual {} from family {} represented in PED but not in VCF. Please remove {}\t{} from input PED.", person.getName(), person.getPedigree(), person.getPedigree(), person.getName())
            );
            throw new PedigreeCreationException("VCF - PED mismatch. There are " + unrepresentedPeople.size() + " individuals in the PED file unrepresented in the VCF");
        }

    }

    private Pedigree buildPedigree(PedFileContents pedFileContents) {
        final String name = pedFileContents.getIndividuals().get(0).getPedigree();
        try {
            logger.info("Building pedigree for family {}", name);
            return new Pedigree(name, new PedigreeExtractor(name, pedFileContents).run());
        } catch (PedParseException e) {
            throw new PedigreeCreationException("Problem parsing the PED file.", e);
        }
    }

    public class PedigreeCreationException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public PedigreeCreationException() {
            super("Error creating Pedigree");
        }

        public PedigreeCreationException(String message) {
            super(message);
        }

        public PedigreeCreationException(String message, Exception e) {
            super(message, e);
        }
    }
}
