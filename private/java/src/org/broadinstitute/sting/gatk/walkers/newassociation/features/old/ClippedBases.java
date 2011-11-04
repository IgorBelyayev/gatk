package org.broadinstitute.sting.gatk.walkers.newassociation.features.old;

import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import org.broadinstitute.sting.utils.sam.GATKSAMRecord;
import org.broadinstitute.sting.gatk.walkers.newassociation.RFAArgumentCollection;

/**
 * Created by IntelliJ IDEA.
 * User: chartl
 * Date: 5/4/11
 * Time: 1:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClippedBases {
    // todo -- make a binary feature version of this

    public Integer extractFeature(GATKSAMRecord record) {
        int nClipped = 0;

        for ( CigarElement e : record.getCigar().getCigarElements() ) {
            if ( e.getOperator().equals(CigarOperator.SOFT_CLIP) || e.getOperator().equals(CigarOperator.HARD_CLIP) ) {
                nClipped += e.getLength();
            }
        }

        return nClipped;
    }

    public boolean featureDefined(GATKSAMRecord rec) { return true; }

    public ClippedBases(RFAArgumentCollection col) {
        //super(col);
    }
}
