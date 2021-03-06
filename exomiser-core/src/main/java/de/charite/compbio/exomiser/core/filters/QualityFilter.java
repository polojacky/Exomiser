package de.charite.compbio.exomiser.core.filters;

import de.charite.compbio.exomiser.core.model.VariantEvaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VariantFilter Variants on the basis of the PHRED quality score for the
 * variant that was derived from the VCF file (QUAL field).
 *
 * @author Peter N Robinson
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 * @version 0.09 (18 December, 2013).
 */
public class QualityFilter implements VariantFilter {

    private static final Logger logger = LoggerFactory.getLogger(QualityFilter.class);

    private static final FilterType filterType = FilterType.QUALITY_FILTER;

    private final FilterResult passesFilter = new PassFilterResult(filterType);
    private final FilterResult failsFilter = new FailFilterResult(filterType);

    /**
     * Threshold for filtering. Retain only those variants whose PHRED variant
     * call quality is at least as good. The default is 1.
     */
    private final double mimimumQualityThreshold;

    /**
     * Constructs a VariantFilter for removing variants which do not pass the
     * defined PHRED score.
     *
     * n.b. We are no longer filtering by requiring a minimum number of reads
     * for each DP4 field (alt/ref in both directions). Instead, we are just
     * filtering on the overall PHRED variant call quality.
     *
     * @param mimimumQualityThreshold The minimum PHRED quality threshold (e.g.
     * 30) under which a variant will be filtered out.
     */
    public QualityFilter(double mimimumQualityThreshold) {
        if (mimimumQualityThreshold <= 0f) {
            throw new IllegalArgumentException(String.format("Illegal value for minimum quality threshold: %2f. Minimum quality threshold must be greater than 0.0", mimimumQualityThreshold));
        }
        this.mimimumQualityThreshold = mimimumQualityThreshold;
    }

    public double getMimimumQualityThreshold() {
        return mimimumQualityThreshold;
    }

    /**
     * Flag for output field representing the QUAL column of the VCF file.
     */
    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public FilterResult runFilter(VariantEvaluation variantEvaluation) {
        double phredScore = variantEvaluation.getPhredScore();
        if (overQualityThreshold(phredScore)) {
            return passesFilter;
        }
        // Variant is not of good quality
        return failsFilter;
    }

    protected boolean overQualityThreshold(double qualityScore) {
        return qualityScore >= mimimumQualityThreshold;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.mimimumQualityThreshold) ^ (Double.doubleToLongBits(this.mimimumQualityThreshold) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QualityFilter other = (QualityFilter) obj;
        if (Double.doubleToLongBits(this.mimimumQualityThreshold) != Double.doubleToLongBits(other.mimimumQualityThreshold)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "QualityFilter{" + "mimimumQualityThreshold=" + mimimumQualityThreshold + '}';
    }

}
