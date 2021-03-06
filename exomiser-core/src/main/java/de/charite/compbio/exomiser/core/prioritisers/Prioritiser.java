package de.charite.compbio.exomiser.core.prioritisers;

import de.charite.compbio.exomiser.core.analysis.AnalysisStep;
import java.util.List;

import de.charite.compbio.exomiser.core.model.Gene;

/**
 * This interface is implemented by classes that perform prioritization of genes
 * (i.e., {@link de.charite.compbio.exomiser.exome.Gene Gene} objects). In contrast to the classes
 * that implement {@code de.charite.compbio.exomiser.filter.Filter}, which remove variants from
 * further consideration (e.g., because they are not predicted to be at all
 * pathogenic), FilterType is inteded to work on genes (predict the relevance of
 * the gene to the disease, without taking the nature or pathogenicity of any
 * variant into account).
 * <P>
 * It is expected that the Exomizer will combine the evaluations of the Filter
 * and the FilterType evaluations in order to reach a final ranking of the genes
 * and variants into candidate disease-causing mutations.
 *
 * @author Peter N Robinson
 * @version 0.13 (13 May, 2013).
 * @see de.charite.compbio.exomiser.filter.Filter
 */
public interface Prioritiser extends AnalysisStep {

    /**
     * Apply a prioritization algorithm to a list of
     * {@link de.charite.compbio.exomiser.exome.Gene Gene} objects. This will have the side effect
     * of setting the Class variable {@link de.charite.compbio.exomiser.exome.Gene#priorityScore}
     * correspondingly. This, together with the filter scores of the {@link jannovar.exome.Variant Variant}
     * {@link de.charite.compbio.exomiser.core.model.Gene Gene} objects can then be used to sort the
     * {@link de.charite.compbio.exomiser.core.model.Gene Gene} objects.
     * <p>
     * Note that this may result in the removal of
     * {@link de.charite.compbio.exomiser.exome.Gene Gene} objects if they do not conform to the
     * Prioritizer.
     *
     * @param genes
     */
    public void prioritizeGenes(List<Gene> genes);

    /**
     * @return an enum constant representing the type of the implementing class.
     */
    public PriorityType getPriorityType();

}
