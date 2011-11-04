package org.broadinstitute.sting.gatk.walkers.newassociation.features.old;

import org.broadinstitute.sting.utils.sam.GATKSAMRecord;
import org.broadinstitute.sting.gatk.walkers.newassociation.RFAArgumentCollection;

/**
 * Created by IntelliJ IDEA.
 * User: chartl
 * Date: 5/4/11
 * Time: 12:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class InsertSize {
    // todo -- this is deprecated by AIS, so the extension is removed.

    public InsertSize(RFAArgumentCollection col) {
        //super(col);
    }

    protected Integer extractFeature(GATKSAMRecord record) {
        return Math.abs(record.getInferredInsertSize());
    }

    protected boolean featureDefined(GATKSAMRecord record) {
        return record.getReadPairedFlag() && record.getProperPairFlag();
    }
}
