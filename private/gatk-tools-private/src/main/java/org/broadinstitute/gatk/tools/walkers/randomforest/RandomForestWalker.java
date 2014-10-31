/*
* By downloading the PROGRAM you agree to the following terms of use:
* 
* BROAD INSTITUTE
* SOFTWARE LICENSE AGREEMENT
* FOR ACADEMIC NON-COMMERCIAL RESEARCH PURPOSES ONLY
* 
* This Agreement is made between the Broad Institute, Inc. with a principal address at 415 Main Street, Cambridge, MA 02142 (“BROAD”) and the LICENSEE and is effective at the date the downloading is completed (“EFFECTIVE DATE”).
* 
* WHEREAS, LICENSEE desires to license the PROGRAM, as defined hereinafter, and BROAD wishes to have this PROGRAM utilized in the public interest, subject only to the royalty-free, nonexclusive, nontransferable license rights of the United States Government pursuant to 48 CFR 52.227-14; and
* WHEREAS, LICENSEE desires to license the PROGRAM and BROAD desires to grant a license on the following terms and conditions.
* NOW, THEREFORE, in consideration of the promises and covenants made herein, the parties hereto agree as follows:
* 
* 1. DEFINITIONS
* 1.1 PROGRAM shall mean copyright in the object code and source code known as GATK3 and related documentation, if any, as they exist on the EFFECTIVE DATE and can be downloaded from http://www.broadinstitute.org/gatk on the EFFECTIVE DATE.
* 
* 2. LICENSE
* 2.1 Grant. Subject to the terms of this Agreement, BROAD hereby grants to LICENSEE, solely for academic non-commercial research purposes, a non-exclusive, non-transferable license to: (a) download, execute and display the PROGRAM and (b) create bug fixes and modify the PROGRAM. LICENSEE hereby automatically grants to BROAD a non-exclusive, royalty-free, irrevocable license to any LICENSEE bug fixes or modifications to the PROGRAM with unlimited rights to sublicense and/or distribute.  LICENSEE agrees to provide any such modifications and bug fixes to BROAD promptly upon their creation.
* The LICENSEE may apply the PROGRAM in a pipeline to data owned by users other than the LICENSEE and provide these users the results of the PROGRAM provided LICENSEE does so for academic non-commercial purposes only. For clarification purposes, academic sponsored research is not a commercial use under the terms of this Agreement.
* 2.2 No Sublicensing or Additional Rights. LICENSEE shall not sublicense or distribute the PROGRAM, in whole or in part, without prior written permission from BROAD. LICENSEE shall ensure that all of its users agree to the terms of this Agreement. LICENSEE further agrees that it shall not put the PROGRAM on a network, server, or other similar technology that may be accessed by anyone other than the LICENSEE and its employees and users who have agreed to the terms of this agreement.
* 2.3 License Limitations. Nothing in this Agreement shall be construed to confer any rights upon LICENSEE by implication, estoppel, or otherwise to any computer software, trademark, intellectual property, or patent rights of BROAD, or of any other entity, except as expressly granted herein. LICENSEE agrees that the PROGRAM, in whole or part, shall not be used for any commercial purpose, including without limitation, as the basis of a commercial software or hardware product or to provide services. LICENSEE further agrees that the PROGRAM shall not be copied or otherwise adapted in order to circumvent the need for obtaining a license for use of the PROGRAM.
* 
* 3. PHONE-HOME FEATURE
* LICENSEE expressly acknowledges that the PROGRAM contains an embedded automatic reporting system (“PHONE-HOME”) which is enabled by default upon download. Unless LICENSEE requests disablement of PHONE-HOME, LICENSEE agrees that BROAD may collect limited information transmitted by PHONE-HOME regarding LICENSEE and its use of the PROGRAM.  Such information shall include LICENSEE’S user identification, version number of the PROGRAM and tools being run, mode of analysis employed, and any error reports generated during run-time.  Collection of such information is used by BROAD solely to monitor usage rates, fulfill reporting requirements to BROAD funding agencies, drive improvements to the PROGRAM, and facilitate adjustments to PROGRAM-related documentation.
* 
* 4. OWNERSHIP OF INTELLECTUAL PROPERTY
* LICENSEE acknowledges that title to the PROGRAM shall remain with BROAD. The PROGRAM is marked with the following BROAD copyright notice and notice of attribution to contributors. LICENSEE shall retain such notice on all copies. LICENSEE agrees to include appropriate attribution if any results obtained from use of the PROGRAM are included in any publication.
* Copyright 2012-2014 Broad Institute, Inc.
* Notice of attribution: The GATK3 program was made available through the generosity of Medical and Population Genetics program at the Broad Institute, Inc.
* LICENSEE shall not use any trademark or trade name of BROAD, or any variation, adaptation, or abbreviation, of such marks or trade names, or any names of officers, faculty, students, employees, or agents of BROAD except as states above for attribution purposes.
* 
* 5. INDEMNIFICATION
* LICENSEE shall indemnify, defend, and hold harmless BROAD, and their respective officers, faculty, students, employees, associated investigators and agents, and their respective successors, heirs and assigns, (Indemnitees), against any liability, damage, loss, or expense (including reasonable attorneys fees and expenses) incurred by or imposed upon any of the Indemnitees in connection with any claims, suits, actions, demands or judgments arising out of any theory of liability (including, without limitation, actions in the form of tort, warranty, or strict liability and regardless of whether such action has any factual basis) pursuant to any right or license granted under this Agreement.
* 
* 6. NO REPRESENTATIONS OR WARRANTIES
* THE PROGRAM IS DELIVERED AS IS. BROAD MAKES NO REPRESENTATIONS OR WARRANTIES OF ANY KIND CONCERNING THE PROGRAM OR THE COPYRIGHT, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE. BROAD EXTENDS NO WARRANTIES OF ANY KIND AS TO PROGRAM CONFORMITY WITH WHATEVER USER MANUALS OR OTHER LITERATURE MAY BE ISSUED FROM TIME TO TIME.
* IN NO EVENT SHALL BROAD OR ITS RESPECTIVE DIRECTORS, OFFICERS, EMPLOYEES, AFFILIATED INVESTIGATORS AND AFFILIATES BE LIABLE FOR INCIDENTAL OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING, WITHOUT LIMITATION, ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER BROAD SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
* 
* 7. ASSIGNMENT
* This Agreement is personal to LICENSEE and any rights or obligations assigned by LICENSEE without the prior written consent of BROAD shall be null and void.
* 
* 8. MISCELLANEOUS
* 8.1 Export Control. LICENSEE gives assurance that it will comply with all United States export control laws and regulations controlling the export of the PROGRAM, including, without limitation, all Export Administration Regulations of the United States Department of Commerce. Among other things, these laws and regulations prohibit, or require a license for, the export of certain types of software to specified countries.
* 8.2 Termination. LICENSEE shall have the right to terminate this Agreement for any reason upon prior written notice to BROAD. If LICENSEE breaches any provision hereunder, and fails to cure such breach within thirty (30) days, BROAD may terminate this Agreement immediately. Upon termination, LICENSEE shall provide BROAD with written assurance that the original and all copies of the PROGRAM have been destroyed, except that, upon prior written authorization from BROAD, LICENSEE may retain a copy for archive purposes.
* 8.3 Survival. The following provisions shall survive the expiration or termination of this Agreement: Articles 1, 3, 4, 5 and Sections 2.2, 2.3, 7.3, and 7.4.
* 8.4 Notice. Any notices under this Agreement shall be in writing, shall specifically refer to this Agreement, and shall be sent by hand, recognized national overnight courier, confirmed facsimile transmission, confirmed electronic mail, or registered or certified mail, postage prepaid, return receipt requested. All notices under this Agreement shall be deemed effective upon receipt.
* 8.5 Amendment and Waiver; Entire Agreement. This Agreement may be amended, supplemented, or otherwise modified only by means of a written instrument signed by all parties. Any waiver of any rights or failure to act in a specific instance shall relate only to such instance and shall not be construed as an agreement to waive any rights or fail to act in any other instance, whether or not similar. This Agreement constitutes the entire agreement among the parties with respect to its subject matter and supersedes prior agreements or understandings between the parties relating to its subject matter.
* 8.6 Binding Effect; Headings. This Agreement shall be binding upon and inure to the benefit of the parties and their respective permitted successors and assigns. All headings are for convenience only and shall not affect the meaning of any provision of this Agreement.
* 8.7 Governing Law. This Agreement shall be construed, governed, interpreted and applied in accordance with the internal laws of the Commonwealth of Massachusetts, U.S.A., without regard to conflict of laws principles.
*/

package org.broadinstitute.gatk.tools.walkers.randomforest;

import org.broadinstitute.gatk.utils.commandline.*;
import org.broadinstitute.gatk.engine.CommandLineGATK;
import org.broadinstitute.gatk.utils.contexts.AlignmentContext;
import org.broadinstitute.gatk.utils.contexts.ReferenceContext;
import org.broadinstitute.gatk.utils.refdata.RefMetaDataTracker;
import org.broadinstitute.gatk.engine.walkers.PartitionBy;
import org.broadinstitute.gatk.engine.walkers.PartitionType;
import org.broadinstitute.gatk.engine.walkers.RodWalker;
import org.broadinstitute.gatk.engine.walkers.TreeReducible;
import org.broadinstitute.gatk.tools.walkers.variantrecalibration.*;
import org.broadinstitute.gatk.utils.GenomeLocParser;
import org.broadinstitute.gatk.utils.collections.ExpandingArrayList;
import org.broadinstitute.gatk.utils.help.DocumentedGATKFeature;
import org.broadinstitute.gatk.utils.help.HelpConstants;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;

import java.io.PrintStream;
import java.util.*;

/**
 * Random-forest-based classification of putative genetic variants.
 * This walker is only designed to be used with raw input mutation callsets coming from the UnifiedGenotyper or HaplotypeCaller.
 * User: rpoplin
 * Date: 11/14/13
 */

@DocumentedGATKFeature( groupName = HelpConstants.DOCS_CAT_VARDISC, extraDocs = {CommandLineGATK.class} )
@PartitionBy(PartitionType.NONE)
public class RandomForestWalker extends RodWalker<ExpandingArrayList<RandomForestDatum>, ExpandingArrayList<RandomForestDatum>> implements TreeReducible<ExpandingArrayList<RandomForestDatum>>  {

    //---------------------------------------------------------------------------------------------------------------
    //
    // inputs
    //
    //---------------------------------------------------------------------------------------------------------------

    @Input(fullName="variant", shortName = "V", doc="The raw input variants to be recalibrated", required=true)
    public List<RodBindingCollection<VariantContext>> variantsCollections = Collections.emptyList();
    final private List<RodBinding<VariantContext>> variants = new ArrayList<>();

    @Input(fullName="aggregate", shortName = "aggregate", doc="Additional raw input variants to be used for modeling only. These variants will not appear in the output.", required=false)
    public List<RodBindingCollection<VariantContext>> aggregateCollections = Collections.emptyList();
    final private List<RodBinding<VariantContext>> aggregate = new ArrayList<>();

    @Input(fullName="good", shortName = "good", doc="The good training labels", required=true)
    public List<RodBindingCollection<VariantContext>> goodTrainingLabelsCollections = Collections.emptyList();
    final private List<RodBinding<VariantContext>> goodTrainingLabels = new ArrayList<>();

    @Input(fullName="bad", shortName = "bad", doc="The bad training labels", required=true)
    public List<RodBindingCollection<VariantContext>> badTrainingLabelsCollections = Collections.emptyList();
    final private List<RodBinding<VariantContext>> badTrainingLabels = new ArrayList<>();

    //---------------------------------------------------------------------------------------------------------------
    //
    // outputs
    //
    //---------------------------------------------------------------------------------------------------------------

    @Output(fullName="recal_file", shortName="recalFile", doc="The output recal file used by ApplyRecalibration", required=true)
    protected VariantContextWriter recalWriter = null;

    @Output(fullName="snp_tranches_file", shortName="snpTranchesFile", doc="The output snp tranches file used by ApplyRecalibration", required=true)
    private PrintStream SNPTranchesStream;

    @Output(fullName="indel_tranches_file", shortName="indelTranchesFile", doc="The output indel tranches file used by ApplyRecalibration", required=true)
    private PrintStream IndelTranchesStream;

    //---------------------------------------------------------------------------------------------------------------
    //
    // command line arguments
    //
    //---------------------------------------------------------------------------------------------------------------

    @Argument(fullName="numTrees", shortName = "numTrees", doc="Number of trees to build. Accuracy versus runtime tradeoff.", required=false)
    public int NUM_TREES = 1000;

    @Argument(fullName="dontExcludeNA12878", shortName = "dontExcludeNA12878", doc="If specified, will not automatically exclude sites which are variable in the sample NA12878", required=false)
    public boolean DONT_EXCLUDE_NA12878 = false;

    private GenomeLocParser genomeLocParser;

    //---------------------------------------------------------------------------------------------------------------
    //
    // initialize
    //
    //---------------------------------------------------------------------------------------------------------------

    @Override
    public void initialize() {
        final Set<VCFHeaderLine> hInfo = new HashSet<>();
        ApplyRecalibration.addVQSRStandardHeaderLines(hInfo);
        recalWriter.writeHeader( new VCFHeader(hInfo) );
        genomeLocParser = getToolkit().getGenomeLocParser();

        // collect the actual rod bindings into a list for use later
        for ( final RodBindingCollection<VariantContext> variantsCollection : variantsCollections )
            variants.addAll(variantsCollection.getRodBindings());
        for ( final RodBindingCollection<VariantContext> aggregateCollection : aggregateCollections )
            aggregate.addAll(aggregateCollection.getRodBindings());
        for ( final RodBindingCollection<VariantContext> goodTrainingLabelsCollection : goodTrainingLabelsCollections )
            goodTrainingLabels.addAll(goodTrainingLabelsCollection.getRodBindings());
        for ( final RodBindingCollection<VariantContext> badTrainingLabelsCollection : badTrainingLabelsCollections )
            badTrainingLabels.addAll(badTrainingLabelsCollection.getRodBindings());

    }

    //---------------------------------------------------------------------------------------------------------------
    //
    // map
    //
    //---------------------------------------------------------------------------------------------------------------

    @Override
    public ExpandingArrayList<RandomForestDatum> map( final RefMetaDataTracker tracker, final ReferenceContext ref, final AlignmentContext context ) {
        final ExpandingArrayList<RandomForestDatum> mapList = new ExpandingArrayList<>();

        if( tracker == null ) { // For some reason RodWalkers get map calls with null trackers
            return mapList;
        }

        final List<VariantContext> goodVCs = new ExpandingArrayList<>();
        final List<VariantContext> badVCs = new ExpandingArrayList<>();

        boolean isTruthSite = false;
        for( final RodBinding<VariantContext> goodRod : goodTrainingLabels ) {
            final List<VariantContext> goodVCsForThisRod = tracker.getValues(goodRod, context.getLocation());
            if( !goodVCsForThisRod.isEmpty() ) {
                goodVCs.addAll( goodVCsForThisRod );
                if( goodRod.getTags().containsKey("tranche") && goodRod.getTags().getValue("tranche").equals("true") ) {
                    isTruthSite = true;
                }
            }
        }
        for( final RodBinding<VariantContext> badRod : badTrainingLabels ) {
            badVCs.addAll( tracker.getValues(badRod, context.getLocation()) );
        }

        parseInputVariantRods(tracker, context, variants, true, isTruthSite, goodVCs, badVCs, mapList);
        parseInputVariantRods(tracker, context, aggregate, false, isTruthSite, goodVCs, badVCs, mapList);

        return mapList;
    }

    /**
     * Loop over the input rods and pull out the VariantContexts that pass that are usable for the RandomForest
     * @param tracker           the tracker from the RodWalker
     * @param context           the AlignmentContext from the RodWalker
     * @param rods              the input rods name
     * @param isInput           is this an input rod (as opposed to the aggregate rod)
     * @param isTruthSite       is this a truth site
     * @param goodVCs           the overlapping good VCs
     * @param badVCs            the overlapping bad VCs
     * @param randomForestData  the list which is accumulating all the RandomForest data
     */
    private void parseInputVariantRods( final RefMetaDataTracker tracker, final AlignmentContext context,
                                        final List<RodBinding<VariantContext>> rods, final boolean isInput, final boolean isTruthSite,
                                        final List<VariantContext> goodVCs, final List<VariantContext> badVCs,
                                        final ExpandingArrayList<RandomForestDatum> randomForestData ) {

        for( final VariantContext vc : tracker.getValues(rods, context.getLocation()) ) {
            if( vc != null && vc.isNotFiltered() ) {
                final boolean isNA12878Variant = !DONT_EXCLUDE_NA12878 && vc.hasGenotype("NA12878") && vc.subContextFromSample("NA12878").isVariant();
                randomForestData.add(new RandomForestDatum(vc, isInput, genomeLocParser, goodVCs, badVCs, isTruthSite, isNA12878Variant));
            }
        }
    }

    //---------------------------------------------------------------------------------------------------------------
    //
    // reduce
    //
    //---------------------------------------------------------------------------------------------------------------

    @Override
    public ExpandingArrayList<RandomForestDatum> reduceInit() {
        return new ExpandingArrayList<>();
    }

    @Override
    public ExpandingArrayList<RandomForestDatum> reduce( final ExpandingArrayList<RandomForestDatum> mapValue, final ExpandingArrayList<RandomForestDatum> reduceSum ) {
        reduceSum.addAll( mapValue );
        return reduceSum;
    }

    @Override
    public ExpandingArrayList<RandomForestDatum> treeReduce( final ExpandingArrayList<RandomForestDatum> lhs, final ExpandingArrayList<RandomForestDatum> rhs ) {
        rhs.addAll( lhs );
        return rhs;
    }


    //---------------------------------------------------------------------------------------------------------------
    //
    // on traversal done
    //
    //---------------------------------------------------------------------------------------------------------------

    @Override
    public void onTraversalDone( final ExpandingArrayList<RandomForestDatum> reduceSum ) {
        summarizeDataByType(reduceSum);

        LinkedHashSet<String> masterKeySet = new LinkedHashSet<>();
        for( final RandomForestDatum rfd : reduceSum ) {
            masterKeySet.addAll(rfd.annotations.keySet());
        }

        logger.info("Master key set for all variants = " + masterKeySet);

        final List<RandomForestDatum> trainingData = RandomForest.subsetToTrainingData(reduceSum);
        final RandomForest classifier = new RandomForest(trainingData, masterKeySet, NUM_TREES);

        final List<RandomForestDatum> inputData = RandomForest.subsetToInputData(reduceSum);
        logger.info( "Evaluating full set of " + inputData.size() + " input variants..." );
        evaluateData( inputData, classifier );

        logger.info("Writing out recalibration table...");
        writeOutRecalibrationTable(recalWriter, inputData);
        logger.info("Writing out tranches file...");
        writeOutTranchesFile(SNPTranchesStream, RandomForest.subsetToSNPs(RandomForest.subsetToInputData(RandomForest.subsetToTruthData(trainingData)), true), VariantRecalibratorArgumentCollection.Mode.SNP);
        writeOutTranchesFile(IndelTranchesStream, RandomForest.subsetToSNPs(RandomForest.subsetToInputData(RandomForest.subsetToTruthData(trainingData)), false), VariantRecalibratorArgumentCollection.Mode.INDEL);
    }

    /**
     * Evaluate each data point using the provided random forest classifier and assign a new VQSLOD score
     * @param data          the data to evaluate
     * @param classifier    the random forest classifier
     */
    private static void evaluateData( final List<RandomForestDatum> data, final RandomForest classifier ) {
        if( data == null ) { throw new IllegalArgumentException("input data set cannot be null"); }
        if( classifier == null ) { throw new IllegalArgumentException("classifier cannot be null"); }
        for( final RandomForestDatum datum : data ) {
            datum.setScore(classifier.classifyDatum(datum));
        }
    }

    /**
     * Write out the recal data file which will be used by ApplyRecalibration
     * @param recalWriter   an ouptut VCF file which contains every variant and their new VQSLOD score
     * @param data          the data to write out
     */
    private void writeOutRecalibrationTable( final VariantContextWriter recalWriter, final List<RandomForestDatum> data ) {
        // we need to sort in coordinate order in order to produce a valid VCF
        Collections.sort( data, new Comparator<RandomForestDatum>() {
            public int compare(final RandomForestDatum vd1, final RandomForestDatum vd2) {
                return vd1.loc.compareTo(vd2.loc);
            }} );

        // create dummy alleles to be used
        final List<Allele> alleles = Arrays.asList(Allele.create("N", true), Allele.create("<VQSR>", false));

        for( final RandomForestDatum datum : data ) {
            final VariantContextBuilder builder = new VariantContextBuilder("VQSR", datum.loc.getContig(), datum.loc.getStart(), datum.loc.getStop(), alleles);
            builder.attribute(VCFConstants.END_KEY, datum.loc.getStop());
            builder.attribute(VariantRecalibrator.VQS_LOD_KEY, String.format("%.4f", datum.score));
            builder.attribute(VariantRecalibrator.CULPRIT_KEY, "NULL"); // TODO -- something meaningful to put here?

            if ( datum.isGood && !datum.isBad ) builder.attribute(VariantRecalibrator.POSITIVE_LABEL_KEY, true);
            if ( datum.isBad && !datum.isGood ) builder.attribute(VariantRecalibrator.NEGATIVE_LABEL_KEY, true);

            recalWriter.add(builder.make());
        }
    }

    /**
     * Calculate the tranches and write them out to disk
     * @param tranchesStream    the tranches file output stream
     * @param data              the data to use to calculate the tranches
     * @param mode              are we in SNP tranche mode or INDEL tranche mode?
     */
    private void writeOutTranchesFile( final PrintStream tranchesStream, final List<RandomForestDatum> data, VariantRecalibratorArgumentCollection.Mode mode) {

        final List<RandomForestDatum> goodData = RandomForest.subsetToSpecificTrainingStatus(data, true);

        // Sort the data by lod score
        Collections.sort( goodData, new Comparator<RandomForestDatum>() {
            public int compare(final RandomForestDatum vd1, final RandomForestDatum vd2) {
                return vd1.score.compareTo(vd2.score);
            }} );

        final List<Tranche> tranches = new ArrayList<>();
        if( goodData.size() > 0 ) {
            for( final double sensitivity : new double[]{0.1, 0.5, 0.6, 0.65, 0.7, 0.75, 0.8, 0.84, 0.85, 0.86, 0.88, 0.9, 0.91, 0.92, 0.93, 0.94, 0.95, 0.96, 0.97, 0.98, 0.99, 0.991, 0.992, 0.993, 0.994, 0.995, 0.996, 0.997, 0.998, 0.999, 1.0} ) {
                final int varIndex = (int)Math.floor( (1.0 - sensitivity) * goodData.size() );
                final double lod = goodData.get(varIndex).score;
                tranches.add(new Tranche( sensitivity * 100.0, lod, 0, 0.0, 0, 0.0, goodData.size(), goodData.size() - varIndex, mode));
            }
        }
        tranchesStream.print(Tranche.tranchesString(tranches));
    }

    /**
     * Write out to the logger a simple summary of the incoming data
     * @param reduceSum the incoming data
     */
    private void summarizeDataByType(final ExpandingArrayList<RandomForestDatum> reduceSum) {
        logger.info(String.format("%d total input variants found", reduceSum.size()));
        for( final VariantContext.Type type : VariantContext.Type.values() ) {
            int numTotal = 0;
            int numGood = 0;
            int numBad = 0;
            int numBoth = 0;

            for( final RandomForestDatum rfd : reduceSum ) {
                if( rfd.type.equals(type) ) {
                    numTotal++;
                    if( rfd.isGood && !rfd.isBad ) { numGood++; }
                    if( rfd.isBad && !rfd.isGood ) { numBad++; }
                    if( rfd.isGood && rfd.isBad ) { numBoth++; }
                }
            }

            if( numTotal > 0 ) {
                logger.info("\t" + type.toString() + ":");
                logger.info("\t\t" + String.format("%d total", numTotal));
                logger.info("\t\t" + String.format("%d good", numGood));
                logger.info("\t\t" + String.format("%d bad", numBad));
                logger.info("\t\t" + String.format("%d labeled both good and bad", numBoth));
            }
        }
    }
}
