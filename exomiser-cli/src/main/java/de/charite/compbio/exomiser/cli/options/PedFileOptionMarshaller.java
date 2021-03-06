/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.cli.options;

import de.charite.compbio.exomiser.core.analysis.Settings.SettingsBuilder;
import java.nio.file.Paths;
import org.apache.commons.cli.OptionBuilder;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class PedFileOptionMarshaller extends AbstractOptionMarshaller {

    public static final String PED_OPTION = "ped";

    public PedFileOptionMarshaller() {
        option = OptionBuilder
                .withArgName("file")
                .hasArg()
                .withDescription("Path to pedigree (ped) file. Required if the vcf file is for a family.")
                .withLongOpt(PED_OPTION)
                .create("p");
    }

    @Override
    public void applyValuesToSettingsBuilder(String[] values, SettingsBuilder settingsBuilder) {
        settingsBuilder.pedFilePath(Paths.get(values[0]));
    }
    
}
