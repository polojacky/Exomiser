/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.cli.options;

import de.charite.compbio.exomiser.core.analysis.Settings.SettingsBuilder;
import org.apache.commons.cli.Option;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class QualityThresholdOptionMarshaller extends AbstractOptionMarshaller {

    public static final String MIN_QUAL_OPTION = "min-qual";

    public QualityThresholdOptionMarshaller() {
        option = new Option("Q", MIN_QUAL_OPTION, true, "Mimimum quality threshold for variants as specifed in VCF 'QUAL' column.  Default: 0");
    }

    @Override
    public void applyValuesToSettingsBuilder(String[] values, SettingsBuilder settingsBuilder) {
        settingsBuilder.minimumQuality(Float.parseFloat(values[0]));
    }    
    
}
