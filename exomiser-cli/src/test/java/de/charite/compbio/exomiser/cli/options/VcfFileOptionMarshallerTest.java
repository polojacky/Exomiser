/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.cli.options;

import de.charite.compbio.exomiser.core.analysis.Settings;
import java.nio.file.Paths;
import org.apache.commons.cli.Option;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class VcfFileOptionMarshallerTest {
    
    VcfFileOptionMarshaller instance;
    
    public VcfFileOptionMarshallerTest() {
    }
    
    @Before
    public void setUp() {
        instance = new VcfFileOptionMarshaller();
    }

    @Test
    public void testGetCommandLineParameter() {
        assertThat(instance.getCommandLineParameter(), equalTo("vcf"));
    }

    @Test
    public void testOptionTakesAnArgument() {
        Option option = instance.getOption();
        assertThat(option.hasArg(), is(true));
    }
    
    @Test
    public void testApplyValuesToSettingsBuilderSetsVcfValue() {
        String vcfFileName = "123.vcf";
        String[] values = {vcfFileName};
        
        Settings.SettingsBuilder settingsBuilder = new Settings.SettingsBuilder();
        instance.applyValuesToSettingsBuilder(values, settingsBuilder);
        Settings settings = settingsBuilder.build();
        
        assertThat(settings.getVcfPath(), equalTo(Paths.get(vcfFileName)));
    }
    
}
