/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.core.filters;

import java.util.LinkedHashSet;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jj8
 */
public class BedFilterTest {
    
    private BedFilter instance;
    private Set<String> targetGeneSymbols;

    
    @Before
    public void setUp() {
        targetGeneSymbols = new LinkedHashSet<>();
        targetGeneSymbols.add("GENE1");

        instance = new BedFilter(targetGeneSymbols);
    }

    @Test
    public void testGetFilterType() {
        assertThat(instance.getFilterType(), equalTo(FilterType.BED_FILTER));
    }

    @Test
    public void testCanGetGeneIds() {
        assertThat(instance.getTargetGeneSymbols(), equalTo(targetGeneSymbols));
    }
    
    @Test
    public void testRunFilter() {
    }

    @Test
    public void testHashCode() {
    }

    @Test
    public void testEquals() {
    }

    @Test
    public void testToString() {
        assertThat(instance.toString(), equalTo("BedFilter{targetGeneSymbols=[GENE1]}"));
    }
    
}
