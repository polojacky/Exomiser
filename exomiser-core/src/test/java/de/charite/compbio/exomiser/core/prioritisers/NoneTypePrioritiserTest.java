/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.core.prioritisers;

import java.util.Collections;
import java.util.List;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class NoneTypePrioritiserTest {
    
    private NoneTypePrioritiser instance;
    
    @Before
    public void setUp() {
        instance = new NoneTypePrioritiser();
    }

    @Test
    public void testRunAnalysisHasNoEffectOnGenes() {
        instance.prioritizeGenes(null);
    }

    @Test
    public void testGetPriorityTypeReturnsNoneType() {
        assertThat(instance.getPriorityType(), equalTo(PriorityType.NONE));
    }
    
    @Test
    public void testToString(){
        assertThat(instance.toString(), equalTo("NoneTypePrioritiser{}"));
    }
}
