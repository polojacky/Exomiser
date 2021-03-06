/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.core.filters;

import de.charite.compbio.exomiser.core.model.FilterStatus;
import de.charite.compbio.exomiser.core.model.Gene;
import de.charite.compbio.exomiser.core.model.VariantEvaluation;
import de.charite.compbio.jannovar.pedigree.ModeOfInheritance;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class SimpleGeneFilterRunnerTest {

    private SimpleGeneFilterRunner instance;

    private InheritanceFilter inheritanceFilter;
    private final ModeOfInheritance PASS_MODE = ModeOfInheritance.AUTOSOMAL_DOMINANT;
    private final ModeOfInheritance FAIL_MODE = ModeOfInheritance.AUTOSOMAL_RECESSIVE;

    private List<GeneFilter> filters;

    private List<Gene> genes;
    private Gene passGene;
    private Gene failGene;

    @Before
    public void setUp() {
        instance = new SimpleGeneFilterRunner();
        inheritanceFilter = new InheritanceFilter(PASS_MODE);

        passGene = makeGeneWithVariants("GENE1", 12345, EnumSet.of(PASS_MODE));

        failGene = makeGeneWithVariants("GENE2", 56789, EnumSet.of(FAIL_MODE));

        filters = new ArrayList<>();
        filters.add(inheritanceFilter);

        genes = new ArrayList<>();
        genes.add(passGene);
        genes.add(failGene);
    }

    private Gene makeGeneWithVariants(String geneSymbol, int geneId, Set<ModeOfInheritance> inheritanceModes) {
        Gene gene = new Gene(geneSymbol, geneId);
        gene.setInheritanceModes(inheritanceModes);
        //Add some variants. For the purposes of this test these are required to
        //have the same inheritance mode as the gene to satisfy the unique bahaviour of the Inheritance filter. 
        //TODO: change this - mock filter required? We're not trying to test the functionality of the InheritanceFilter here.
        gene.addVariant(new VariantEvaluation.VariantBuilder(1, 1, "A", "T").build());
        gene.addVariant(new VariantEvaluation.VariantBuilder(1, 2, "G", "T").build());
        for (VariantEvaluation variantEvaluation : gene.getVariantEvaluations()) {
            variantEvaluation.setInheritanceModes(inheritanceModes);
        }
        return gene;
    }

    private void assertVariantsUnfilteredAndDoNotPassFilter(List<Gene> genes, List<GeneFilter> geneFilters) {
        for (Gene gene : genes) {
            assertThat(gene.passedFilters(), is(true));
            for (VariantEvaluation variantEvaluation : gene.getVariantEvaluations()) {
//                for (GeneFilter filter : geneFilters) {
//                    assertThat(variantEvaluation.passedFilter(filter.getFilterType()), is(false));
//                }
                assertThat(variantEvaluation.getFilterStatus(), equalTo(FilterStatus.UNFILTERED));
            }
        }
    }

    private void assertFilterStatus(Gene gene, List<GeneFilter> filters, FilterStatus filterStatus) {
        boolean hasPassed = true;
        if (filterStatus == FilterStatus.FAILED) {
            hasPassed = false;
        }
        System.out.println(gene);
        for (VariantEvaluation variantEvaluation :  gene.getVariantEvaluations()) {
            System.out.println(variantEvaluation);
        }
       
        assertThat(gene.passedFilters(), equalTo(hasPassed));
        for (GeneFilter filter : filters) {
            FilterType filterType = filter.getFilterType();
            assertThat(gene.passedFilter(filterType), equalTo(hasPassed));
            for (VariantEvaluation variantEvaluation : gene.getVariantEvaluations()) {
//                assertThat(variantEvaluation.passedFilter(filterType), equalTo(hasPassed));
//                assertThat(variantEvaluation.getFilterStatus(), equalTo(filterStatus));
            }
        }
    }

    @Test
    public void testRun_MultipleFiltersOverGenes() {
        assertVariantsUnfilteredAndDoNotPassFilter(genes, filters);

        instance.run(filters, genes);
        
        assertFilterStatus(passGene, filters, FilterStatus.PASSED);
        assertFilterStatus(failGene, filters, FilterStatus.FAILED);
    }

    @Test
    public void testRun_SingleFilterOverGenes() {
        assertVariantsUnfilteredAndDoNotPassFilter(genes, filters);

        instance.run(inheritanceFilter, genes);

        assertFilterStatus(passGene, filters, FilterStatus.PASSED);
        assertFilterStatus(failGene, filters, FilterStatus.FAILED);
    }

}
