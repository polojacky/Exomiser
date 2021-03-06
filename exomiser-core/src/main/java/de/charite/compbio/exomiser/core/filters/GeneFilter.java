/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.core.filters;

import de.charite.compbio.exomiser.core.model.Gene;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public interface GeneFilter extends Filter<Gene> {

    /**
     * True or false depending on whether the {@code Gene} passes the runFilter
     * or not.
     *
     * @param gene to be filtered
     * @return true if the {@code Gene} passes the runFilter.
     */
    @Override
    public FilterResult runFilter(Gene gene);

}
