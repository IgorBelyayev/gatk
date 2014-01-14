/*
* Copyright (c) 2012 The Broad Institute
* 
* Permission is hereby granted, free of charge, to any person
* obtaining a copy of this software and associated documentation
* files (the "Software"), to deal in the Software without
* restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the
* Software is furnished to do so, subject to the following
* conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
* OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
* THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.broadinstitute.sting.utils.variant;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.broad.tribble.TribbleException;
import org.broad.tribble.util.popgen.HardyWeinbergCalculation;
import org.broadinstitute.sting.utils.*;
import org.broadinstitute.sting.utils.collections.Pair;
import org.broadinstitute.sting.utils.exceptions.UserException;
import org.broadinstitute.variant.variantcontext.*;
import org.broadinstitute.variant.vcf.VCFConstants;

import java.io.Serializable;
import java.util.*;

public class GATKVariantContextUtils {

    private static Logger logger = Logger.getLogger(GATKVariantContextUtils.class);

    public static final int DEFAULT_PLOIDY = 2;
    public static final double SUM_GL_THRESH_NOCALL = -0.1; // if sum(gl) is bigger than this threshold, we treat GL's as non-informative and will force a no-call.

    public final static List<Allele> NO_CALL_ALLELES = Arrays.asList(Allele.NO_CALL, Allele.NO_CALL);
    public final static String NON_REF_SYMBOLIC_ALLELE_NAME = "NON_REF";
    public final static Allele NON_REF_SYMBOLIC_ALLELE = Allele.create("<"+NON_REF_SYMBOLIC_ALLELE_NAME+">", false); // represents any possible non-ref allele at this site

    public final static String MERGE_FILTER_PREFIX = "filterIn";
    public final static String MERGE_REF_IN_ALL = "ReferenceInAll";
    public final static String MERGE_FILTER_IN_ALL = "FilteredInAll";
    public final static String MERGE_INTERSECTION = "Intersection";

    public enum GenotypeMergeType {
        /**
         * Make all sample genotypes unique by file. Each sample shared across RODs gets named sample.ROD.
         */
        UNIQUIFY,
        /**
         * Take genotypes in priority order (see the priority argument).
         */
        PRIORITIZE,
        /**
         * Take the genotypes in any order.
         */
        UNSORTED,
        /**
         * Require that all samples/genotypes be unique between all inputs.
         */
        REQUIRE_UNIQUE
    }

    public enum FilteredRecordMergeType {
        /**
         * Union - leaves the record if any record is unfiltered.
         */
        KEEP_IF_ANY_UNFILTERED,
        /**
         * Requires all records present at site to be unfiltered. VCF files that don't contain the record don't influence this.
         */
        KEEP_IF_ALL_UNFILTERED,
        /**
         * If any record is present at this site (regardless of possibly being filtered), then all such records are kept and the filters are reset.
         */
        KEEP_UNCONDITIONAL
    }

    public enum MultipleAllelesMergeType {
        /**
         * Combine only alleles of the same type (SNP, indel, etc.) into a single VCF record.
         */
        BY_TYPE,
        /**
         * Merge all allele types at the same start position into the same VCF record.
         */
        MIX_TYPES
    }

    /**
     * Refactored out of the AverageAltAlleleLength annotation class
     * @param vc the variant context
     * @return the average length of the alt allele (a double)
     */
    public static double getMeanAltAlleleLength(VariantContext vc) {
        double averageLength = 1.0;
        if ( ! vc.isSNP() && ! vc.isSymbolic() ) {
            // adjust for the event length
            int averageLengthNum = 0;
            int averageLengthDenom = 0;
            int refLength = vc.getReference().length();
            for ( final Allele a : vc.getAlternateAlleles() ) {
                int numAllele = vc.getCalledChrCount(a);
                int alleleSize;
                if ( a.length() == refLength ) {
                    // SNP or MNP
                    byte[] a_bases = a.getBases();
                    byte[] ref_bases = vc.getReference().getBases();
                    int n_mismatch = 0;
                    for ( int idx = 0; idx < a_bases.length; idx++ ) {
                        if ( a_bases[idx] != ref_bases[idx] )
                            n_mismatch++;
                    }
                    alleleSize = n_mismatch;
                }
                else if ( a.isSymbolic() ) {
                    alleleSize = 1;
                } else {
                    alleleSize = Math.abs(refLength-a.length());
                }
                averageLengthNum += alleleSize*numAllele;
                averageLengthDenom += numAllele;
            }
            averageLength = ( (double) averageLengthNum )/averageLengthDenom;
        }

        return averageLength;
    }

    /**
     * create a genome location, given a variant context
     * @param genomeLocParser parser
     * @param vc the variant context
     * @return the genomeLoc
     */
    public static final GenomeLoc getLocation(GenomeLocParser genomeLocParser,VariantContext vc) {
        return genomeLocParser.createGenomeLoc(vc.getChr(), vc.getStart(), vc.getEnd(), true);
    }

    public static BaseUtils.BaseSubstitutionType getSNPSubstitutionType(VariantContext context) {
        if (!context.isSNP() || !context.isBiallelic())
            throw new IllegalStateException("Requested SNP substitution type for bialleic non-SNP " + context);
        return BaseUtils.SNPSubstitutionType(context.getReference().getBases()[0], context.getAlternateAllele(0).getBases()[0]);
    }

    /**
     * If this is a BiAllelic SNP, is it a transition?
     */
    public static boolean isTransition(VariantContext context) {
        return getSNPSubstitutionType(context) == BaseUtils.BaseSubstitutionType.TRANSITION;
    }

    /**
     * If this is a BiAllelic SNP, is it a transversion?
     */
    public static boolean isTransversion(VariantContext context) {
        return getSNPSubstitutionType(context) == BaseUtils.BaseSubstitutionType.TRANSVERSION;
    }

    public static boolean isTransition(Allele ref, Allele alt) {
        return BaseUtils.SNPSubstitutionType(ref.getBases()[0], alt.getBases()[0]) == BaseUtils.BaseSubstitutionType.TRANSITION;
    }

    public static boolean isTransversion(Allele ref, Allele alt) {
        return BaseUtils.SNPSubstitutionType(ref.getBases()[0], alt.getBases()[0]) == BaseUtils.BaseSubstitutionType.TRANSVERSION;
    }

    /**
     * Returns a context identical to this with the REF and ALT alleles reverse complemented.
     *
     * @param vc        variant context
     * @return new vc
     */
    public static VariantContext reverseComplement(VariantContext vc) {
        // create a mapping from original allele to reverse complemented allele
        HashMap<Allele, Allele> alleleMap = new HashMap<>(vc.getAlleles().size());
        for ( final Allele originalAllele : vc.getAlleles() ) {
            Allele newAllele;
            if ( originalAllele.isNoCall() )
                newAllele = originalAllele;
            else
                newAllele = Allele.create(BaseUtils.simpleReverseComplement(originalAllele.getBases()), originalAllele.isReference());
            alleleMap.put(originalAllele, newAllele);
        }

        // create new Genotype objects
        GenotypesContext newGenotypes = GenotypesContext.create(vc.getNSamples());
        for ( final Genotype genotype : vc.getGenotypes() ) {
            List<Allele> newAlleles = new ArrayList<>();
            for ( final Allele allele : genotype.getAlleles() ) {
                Allele newAllele = alleleMap.get(allele);
                if ( newAllele == null )
                    newAllele = Allele.NO_CALL;
                newAlleles.add(newAllele);
            }
            newGenotypes.add(new GenotypeBuilder(genotype).alleles(newAlleles).make());
        }

        return new VariantContextBuilder(vc).alleles(alleleMap.values()).genotypes(newGenotypes).make();
    }

    /**
     * Returns true iff VC is an non-complex indel where every allele represents an expansion or
     * contraction of a series of identical bases in the reference.
     *
     * For example, suppose the ref bases are CTCTCTGA, which includes a 3x repeat of CTCTCT
     *
     * If VC = -/CT, then this function returns true because the CT insertion matches exactly the
     * upcoming reference.
     * If VC = -/CTA then this function returns false because the CTA isn't a perfect match
     *
     * Now consider deletions:
     *
     * If VC = CT/- then again the same logic applies and this returns true
     * The case of CTA/- makes no sense because it doesn't actually match the reference bases.
     *
     * The logic of this function is pretty simple.  Take all of the non-null alleles in VC.  For
     * each insertion allele of n bases, check if that allele matches the next n reference bases.
     * For each deletion allele of n bases, check if this matches the reference bases at n - 2 n,
     * as it must necessarily match the first n bases.  If this test returns true for all
     * alleles you are a tandem repeat, otherwise you are not.
     *
     * @param vc
     * @param refBasesStartingAtVCWithPad not this is assumed to include the PADDED reference
     * @return
     */
    @Requires({"vc != null", "refBasesStartingAtVCWithPad != null && refBasesStartingAtVCWithPad.length > 0"})
    public static boolean isTandemRepeat(final VariantContext vc, final byte[] refBasesStartingAtVCWithPad) {
        final String refBasesStartingAtVCWithoutPad = new String(refBasesStartingAtVCWithPad).substring(1);
        if ( ! vc.isIndel() ) // only indels are tandem repeats
            return false;

        final Allele ref = vc.getReference();

        for ( final Allele allele : vc.getAlternateAlleles() ) {
            if ( ! isRepeatAllele(ref, allele, refBasesStartingAtVCWithoutPad) )
                return false;
        }

        // we've passed all of the tests, so we are a repeat
        return true;
    }

    /**
     *
     * @param vc
     * @param refBasesStartingAtVCWithPad
     * @return
     */
    @Requires({"vc != null", "refBasesStartingAtVCWithPad != null && refBasesStartingAtVCWithPad.length > 0"})
    public static Pair<List<Integer>,byte[]> getNumTandemRepeatUnits(final VariantContext vc, final byte[] refBasesStartingAtVCWithPad) {
        final boolean VERBOSE = false;
        final String refBasesStartingAtVCWithoutPad = new String(refBasesStartingAtVCWithPad).substring(1);
        if ( ! vc.isIndel() ) // only indels are tandem repeats
            return null;

        final Allele refAllele = vc.getReference();
        final byte[] refAlleleBases = Arrays.copyOfRange(refAllele.getBases(), 1, refAllele.length());

        byte[] repeatUnit = null;
        final ArrayList<Integer> lengths = new ArrayList<>();

        for ( final Allele allele : vc.getAlternateAlleles() ) {
            Pair<int[],byte[]> result = getNumTandemRepeatUnits(refAlleleBases, Arrays.copyOfRange(allele.getBases(), 1, allele.length()), refBasesStartingAtVCWithoutPad.getBytes());

            final int[] repetitionCount = result.first;
            // repetition count = 0 means allele is not a tandem expansion of context
            if (repetitionCount[0] == 0 || repetitionCount[1] == 0)
                return null;

            if (lengths.size() == 0) {
                lengths.add(repetitionCount[0]); // add ref allele length only once
            }
            lengths.add(repetitionCount[1]);  // add this alt allele's length

            repeatUnit = result.second;
            if (VERBOSE) {
                System.out.println("RefContext:"+refBasesStartingAtVCWithoutPad);
                System.out.println("Ref:"+refAllele.toString()+" Count:" + String.valueOf(repetitionCount[0]));
                System.out.println("Allele:"+allele.toString()+" Count:" + String.valueOf(repetitionCount[1]));
                System.out.println("RU:"+new String(repeatUnit));
            }
        }

        return new Pair<List<Integer>, byte[]>(lengths,repeatUnit);
    }

    public static Pair<int[],byte[]> getNumTandemRepeatUnits(final byte[] refBases, final byte[] altBases, final byte[] remainingRefContext) {
         /* we can't exactly apply same logic as in basesAreRepeated() to compute tandem unit and number of repeated units.
           Consider case where ref =ATATAT and we have an insertion of ATAT. Natural description is (AT)3 -> (AT)2.
         */

        byte[] longB;
        // find first repeat unit based on either ref or alt, whichever is longer
        if (altBases.length > refBases.length)
            longB = altBases;
        else
            longB = refBases;

        // see if non-null allele (either ref or alt, whichever is longer) can be decomposed into several identical tandem units
        // for example, -*,CACA needs to first be decomposed into (CA)2
        final int repeatUnitLength = findRepeatedSubstring(longB);
        final byte[] repeatUnit = Arrays.copyOf(longB, repeatUnitLength);

        final int[] repetitionCount = new int[2];
        // look for repetitions forward on the ref bases (i.e. starting at beginning of ref bases)
        int repetitionsInRef = findNumberofRepetitions(repeatUnit,refBases, true);
        repetitionCount[0] = findNumberofRepetitions(repeatUnit, ArrayUtils.addAll(refBases, remainingRefContext), true)-repetitionsInRef;
        repetitionCount[1] = findNumberofRepetitions(repeatUnit, ArrayUtils.addAll(altBases, remainingRefContext), true)-repetitionsInRef;

        return new Pair<>(repetitionCount, repeatUnit);

    }

    /**
     * Find out if a string can be represented as a tandem number of substrings.
     * For example ACTACT is a 2-tandem of ACT,
     * but ACTACA is not.
     *
     * @param bases                 String to be tested
     * @return                      Length of repeat unit, if string can be represented as tandem of substring (if it can't
     *                              be represented as one, it will be just the length of the input string)
     */
    public static int findRepeatedSubstring(byte[] bases) {

        int repLength;
        for (repLength=1; repLength <=bases.length; repLength++) {
            final byte[] candidateRepeatUnit = Arrays.copyOf(bases,repLength);
            boolean allBasesMatch = true;
            for (int start = repLength; start < bases.length; start += repLength ) {
                // check that remaining of string is exactly equal to repeat unit
                final byte[] basePiece = Arrays.copyOfRange(bases,start,start+candidateRepeatUnit.length);
                if (!Arrays.equals(candidateRepeatUnit, basePiece)) {
                    allBasesMatch = false;
                    break;
                }
            }
            if (allBasesMatch)
                return repLength;
        }

        return repLength;
    }

    /**
     * Helper routine that finds number of repetitions a string consists of.
     * For example, for string ATAT and repeat unit AT, number of repetitions = 2
     * @param repeatUnit             Substring
     * @param testString             String to test
     * @oaram lookForward            Look for repetitions forward (at beginning of string) or backward (at end of string)
     * @return                       Number of repetitions (0 if testString is not a concatenation of n repeatUnit's
     */
    public static int findNumberofRepetitions(byte[] repeatUnit, byte[] testString, boolean lookForward) {
        int numRepeats = 0;
        if (lookForward) {
            // look forward on the test string
            for (int start = 0; start < testString.length; start += repeatUnit.length) {
                int end = start + repeatUnit.length;
                byte[] unit = Arrays.copyOfRange(testString,start, end);
                if(Arrays.equals(unit,repeatUnit))
                    numRepeats++;
                else
                    break;
            }
            return numRepeats;
        }

        // look backward. For example, if repeatUnit = AT and testString = GATAT, number of repeat units is still 2
        // look forward on the test string
        for (int start = testString.length - repeatUnit.length; start >= 0; start -= repeatUnit.length) {
            int end = start + repeatUnit.length;
            byte[] unit = Arrays.copyOfRange(testString,start, end);
            if(Arrays.equals(unit,repeatUnit))
                numRepeats++;
            else
                break;
        }
        return numRepeats;
    }

    /**
     * Helper function for isTandemRepeat that checks that allele matches somewhere on the reference
     * @param ref
     * @param alt
     * @param refBasesStartingAtVCWithoutPad
     * @return
     */
    protected static boolean isRepeatAllele(final Allele ref, final Allele alt, final String refBasesStartingAtVCWithoutPad) {
        if ( ! Allele.oneIsPrefixOfOther(ref, alt) )
            return false; // we require one allele be a prefix of another

        if ( ref.length() > alt.length() ) { // we are a deletion
            return basesAreRepeated(ref.getBaseString(), alt.getBaseString(), refBasesStartingAtVCWithoutPad, 2);
        } else { // we are an insertion
            return basesAreRepeated(alt.getBaseString(), ref.getBaseString(), refBasesStartingAtVCWithoutPad, 1);
        }
    }

    protected static boolean basesAreRepeated(final String l, final String s, final String ref, final int minNumberOfMatches) {
        final String potentialRepeat = l.substring(s.length()); // skip s bases

        for ( int i = 0; i < minNumberOfMatches; i++) {
            final int start = i * potentialRepeat.length();
            final int end = (i+1) * potentialRepeat.length();
            if ( ref.length() < end )
                return false; // we ran out of bases to test
            final String refSub = ref.substring(start, end);
            if ( ! refSub.equals(potentialRepeat) )
                return false; // repeat didn't match, fail
        }

        return true; // we passed all tests, we matched
    }

    public enum GenotypeAssignmentMethod {
        /**
         * set all of the genotype GT values to NO_CALL
         */
        SET_TO_NO_CALL,

        /**
         * Use the subsetted PLs to greedily assigned genotypes
         */
        USE_PLS_TO_ASSIGN,

        /**
         * Try to match the original GT calls, if at all possible
         *
         * Suppose I have 3 alleles: A/B/C and the following samples:
         *
         *       original_GT best_match to A/B best_match to A/C
         * S1 => A/A A/A A/A
         * S2 => A/B A/B A/A
         * S3 => B/B B/B A/A
         * S4 => B/C A/B A/C
         * S5 => C/C A/A C/C
         *
         * Basically, all alleles not in the subset map to ref.  It means that het-alt genotypes
         * when split into 2 bi-allelic variants will be het in each, which is good in some cases,
         * rather than the undetermined behavior when using the PLs to assign, which could result
         * in hom-var or hom-ref for each, depending on the exact PL values.
         */
        BEST_MATCH_TO_ORIGINAL,

        /**
         * do not even bother changing the GTs
         */
        DO_NOT_ASSIGN_GENOTYPES
    }

    /**
     * subset the Variant Context to the specific set of alleles passed in (pruning the PLs appropriately)
     *
     * @param vc                 variant context with genotype likelihoods
     * @param allelesToUse       which alleles from the vc are okay to use; *** must be in the same relative order as those in the original VC ***
     * @param assignGenotypes    assignment strategy for the (subsetted) PLs
     * @return a new non-null GenotypesContext
     */
    public static GenotypesContext subsetDiploidAlleles(final VariantContext vc,
                                                        final List<Allele> allelesToUse,
                                                        final GenotypeAssignmentMethod assignGenotypes) {
        if ( allelesToUse.get(0).isNonReference() ) throw new IllegalArgumentException("First allele must be the reference allele");
        if ( allelesToUse.size() == 1 ) throw new IllegalArgumentException("Cannot subset to only 1 alt allele");

        // optimization: if no input genotypes, just exit
        if (vc.getGenotypes().isEmpty()) return GenotypesContext.create();

        // we need to determine which of the alternate alleles (and hence the likelihoods) to use and carry forward
        final List<Integer> likelihoodIndexesToUse = determineLikelihoodIndexesToUse(vc, allelesToUse);

        // create the new genotypes
        return createGenotypesWithSubsettedLikelihoods(vc.getGenotypes(), vc, allelesToUse, likelihoodIndexesToUse, assignGenotypes);
    }

    /**
     * Figure out which likelihood indexes to use for a selected down set of alleles
     *
     * @param originalVC        the original VariantContext
     * @param allelesToUse      the subset of alleles to use
     * @return a list of PL indexes to use or null if none
     */
    private static List<Integer> determineLikelihoodIndexesToUse(final VariantContext originalVC, final List<Allele> allelesToUse) {

        // the bitset representing the allele indexes we want to keep
        final boolean[] alleleIndexesToUse = getAlleleIndexBitset(originalVC, allelesToUse);

        // an optimization: if we are supposed to use all (or none in the case of a ref call) of the alleles,
        // then we can keep the PLs as is; otherwise, we determine which ones to keep
        if ( MathUtils.countOccurrences(true, alleleIndexesToUse) == alleleIndexesToUse.length )
            return null;

        return getLikelihoodIndexes(originalVC, alleleIndexesToUse);
    }

    /**
     * Get the actual likelihoods indexes to use given the corresponding allele indexes
     *
     * @param originalVC           the original VariantContext
     * @param alleleIndexesToUse   the bitset representing the alleles to use (@see #getAlleleIndexBitset)
     * @return a non-null List
     */
    private static List<Integer> getLikelihoodIndexes(final VariantContext originalVC, final boolean[] alleleIndexesToUse) {

        final List<Integer> result = new ArrayList<>(30);

        // numLikelihoods takes total # of alleles. Use default # of chromosomes (ploidy) = 2
        final int numLikelihoods = GenotypeLikelihoods.numLikelihoods(originalVC.getNAlleles(), DEFAULT_PLOIDY);

        for ( int PLindex = 0; PLindex < numLikelihoods; PLindex++ ) {
            final GenotypeLikelihoods.GenotypeLikelihoodsAllelePair alleles = GenotypeLikelihoods.getAllelePair(PLindex);
            // consider this entry only if both of the alleles are good
            if ( alleleIndexesToUse[alleles.alleleIndex1] && alleleIndexesToUse[alleles.alleleIndex2] )
                result.add(PLindex);
        }

        return result;
    }

    /**
     * Given an original VariantContext and a list of alleles from that VC to keep,
     * returns a bitset representing which allele indexes should be kept
     *
     * @param originalVC      the original VC
     * @param allelesToKeep   the list of alleles to keep
     * @return non-null bitset
     */
    private static boolean[] getAlleleIndexBitset(final VariantContext originalVC, final List<Allele> allelesToKeep) {
        final int numOriginalAltAlleles = originalVC.getNAlleles() - 1;
        final boolean[] alleleIndexesToKeep = new boolean[numOriginalAltAlleles + 1];

        // the reference Allele is definitely still used
        alleleIndexesToKeep[0] = true;
        for ( int i = 0; i < numOriginalAltAlleles; i++ ) {
            if ( allelesToKeep.contains(originalVC.getAlternateAllele(i)) )
                alleleIndexesToKeep[i+1] = true;
        }

        return alleleIndexesToKeep;
    }

    /**
     * Create the new GenotypesContext with the subsetted PLs
     *
     * @param originalGs               the original GenotypesContext
     * @param vc                       the original VariantContext
     * @param allelesToUse             the actual alleles to use with the new Genotypes
     * @param likelihoodIndexesToUse   the indexes in the PL to use given the allelesToUse (@see #determineLikelihoodIndexesToUse())
     * @param assignGenotypes          assignment strategy for the (subsetted) PLs
     * @return a new non-null GenotypesContext
     */
    private static GenotypesContext createGenotypesWithSubsettedLikelihoods(final GenotypesContext originalGs,
                                                                            final VariantContext vc,
                                                                            final List<Allele> allelesToUse,
                                                                            final List<Integer> likelihoodIndexesToUse,
                                                                            final GenotypeAssignmentMethod assignGenotypes) {
        // the new genotypes to create
        final GenotypesContext newGTs = GenotypesContext.create(originalGs.size());

        // make sure we are seeing the expected number of likelihoods per sample
        final int expectedNumLikelihoods = GenotypeLikelihoods.numLikelihoods(vc.getNAlleles(), 2);

        // the samples
        final List<String> sampleIndices = originalGs.getSampleNamesOrderedByName();

        // create the new genotypes
        for ( int k = 0; k < originalGs.size(); k++ ) {
            final Genotype g = originalGs.get(sampleIndices.get(k));
            final GenotypeBuilder gb = new GenotypeBuilder(g);

            // create the new likelihoods array from the alleles we are allowed to use
            double[] newLikelihoods;
            if ( !g.hasLikelihoods() ) {
                // we don't have any likelihoods, so we null out PLs and make G ./.
                newLikelihoods = null;
                gb.noPL();
            } else {
                final double[] originalLikelihoods = g.getLikelihoods().getAsVector();
                if ( likelihoodIndexesToUse == null ) {
                    newLikelihoods = originalLikelihoods;
                } else if ( originalLikelihoods.length != expectedNumLikelihoods ) {
                    logger.warn("Wrong number of likelihoods in sample " + g.getSampleName() + " at " + vc + " got " + g.getLikelihoodsString() + " but expected " + expectedNumLikelihoods);
                    newLikelihoods = null;
                } else {
                    newLikelihoods = new double[likelihoodIndexesToUse.size()];
                    int newIndex = 0;
                    for ( final int oldIndex : likelihoodIndexesToUse )
                        newLikelihoods[newIndex++] = originalLikelihoods[oldIndex];

                    // might need to re-normalize
                    newLikelihoods = MathUtils.normalizeFromLog10(newLikelihoods, false, true);
                }

                if ( newLikelihoods == null || likelihoodsAreUninformative(newLikelihoods) )
                    gb.noPL();
                else
                    gb.PL(newLikelihoods);
            }

            updateGenotypeAfterSubsetting(g.getAlleles(), gb, assignGenotypes, newLikelihoods, allelesToUse);
            newGTs.add(gb.make());
        }

        return newGTs;
    }

    private static boolean likelihoodsAreUninformative(final double[] likelihoods) {
        return MathUtils.sum(likelihoods) > SUM_GL_THRESH_NOCALL;
    }

    /**
     * Add the genotype call (GT) field to GenotypeBuilder using the requested algorithm assignmentMethod
     *
     * @param originalGT the original genotype calls, cannot be null
     * @param gb the builder where we should put our newly called alleles, cannot be null
     * @param assignmentMethod the method to use to do the assignment, cannot be null
     * @param newLikelihoods a vector of likelihoods to use if the method requires PLs, should be log10 likelihoods, cannot be null
     * @param allelesToUse the alleles we are using for our subsetting
     */
    public static void updateGenotypeAfterSubsetting(final List<Allele> originalGT,
                                                     final GenotypeBuilder gb,
                                                     final GenotypeAssignmentMethod assignmentMethod,
                                                     final double[] newLikelihoods,
                                                     final List<Allele> allelesToUse) {
        switch ( assignmentMethod ) {
            case DO_NOT_ASSIGN_GENOTYPES:
                break;
            case SET_TO_NO_CALL:
                gb.alleles(NO_CALL_ALLELES);
                gb.noAD();
                gb.noGQ();
                break;
            case USE_PLS_TO_ASSIGN:
                gb.noAD();
                if ( newLikelihoods == null || likelihoodsAreUninformative(newLikelihoods) ) {
                    // if there is no mass on the (new) likelihoods, then just no-call the sample
                    gb.alleles(NO_CALL_ALLELES);
                    gb.noGQ();
                } else {
                    // find the genotype with maximum likelihoods
                    final int PLindex = MathUtils.maxElementIndex(newLikelihoods);
                    GenotypeLikelihoods.GenotypeLikelihoodsAllelePair alleles = GenotypeLikelihoods.getAllelePair(PLindex);
                    gb.alleles(Arrays.asList(allelesToUse.get(alleles.alleleIndex1), allelesToUse.get(alleles.alleleIndex2)));
                    gb.log10PError(GenotypeLikelihoods.getGQLog10FromLikelihoods(PLindex, newLikelihoods));
                }
                break;
            case BEST_MATCH_TO_ORIGINAL:
                final List<Allele> best = new LinkedList<>();
                final Allele ref = allelesToUse.get(0); // WARNING -- should be checked in input argument
                for ( final Allele originalAllele : originalGT ) {
                    best.add(allelesToUse.contains(originalAllele) ? originalAllele : ref);
                }
                gb.noGQ();
                gb.noPL();
                gb.noAD();
                gb.alleles(best);
                break;
        }
    }

    /**
     * Subset the samples in VC to reference only information with ref call alleles
     *
     * Preserves DP if present
     *
     * @param vc the variant context to subset down to
     * @param ploidy ploidy to use if a genotype doesn't have any alleles
     * @return a GenotypesContext
     */
    public static GenotypesContext subsetToRefOnly(final VariantContext vc, final int ploidy) {
        if ( vc == null ) throw new IllegalArgumentException("vc cannot be null");
        if ( ploidy < 1 ) throw new IllegalArgumentException("ploidy must be >= 1 but got " + ploidy);

        // the genotypes with PLs
        final GenotypesContext oldGTs = vc.getGenotypes();

        // optimization: if no input genotypes, just exit
        if (oldGTs.isEmpty()) return oldGTs;

        // the new genotypes to create
        final GenotypesContext newGTs = GenotypesContext.create(oldGTs.size());

        final Allele ref = vc.getReference();
        final List<Allele> diploidRefAlleles = Arrays.asList(ref, ref);

        // create the new genotypes
        for ( final Genotype g : vc.getGenotypes() ) {
            final int gPloidy = g.getPloidy() == 0 ? ploidy : g.getPloidy();
            final List<Allele> refAlleles = gPloidy == 2 ? diploidRefAlleles : Collections.nCopies(gPloidy, ref);
            final GenotypeBuilder gb = new GenotypeBuilder(g.getSampleName(), refAlleles);
            if ( g.hasDP() ) gb.DP(g.getDP());
            if ( g.hasGQ() ) gb.GQ(g.getGQ());
            newGTs.add(gb.make());
        }

        return newGTs;
    }

    /**
     * Assign genotypes (GTs) to the samples in the Variant Context greedily based on the PLs
     *
     * @param vc            variant context with genotype likelihoods
     * @return genotypes context
     */
    public static GenotypesContext assignDiploidGenotypes(final VariantContext vc) {
        return subsetDiploidAlleles(vc, vc.getAlleles(), GenotypeAssignmentMethod.USE_PLS_TO_ASSIGN);
    }

    /**
     * Split variant context into its biallelic components if there are more than 2 alleles
     *
     * For VC has A/B/C alleles, returns A/B and A/C contexts.
     * Genotypes are all no-calls now (it's not possible to fix them easily)
     * Alleles are right trimmed to satisfy VCF conventions
     *
     * If vc is biallelic or non-variant it is just returned
     *
     * Chromosome counts are updated (but they are by definition 0)
     *
     * @param vc a potentially multi-allelic variant context
     * @return a list of bi-allelic (or monomorphic) variant context
     */
    public static List<VariantContext> splitVariantContextToBiallelics(final VariantContext vc) {
        return splitVariantContextToBiallelics(vc, false, GenotypeAssignmentMethod.SET_TO_NO_CALL);
    }

    /**
     * Split variant context into its biallelic components if there are more than 2 alleles
     *
     * For VC has A/B/C alleles, returns A/B and A/C contexts.
     * Genotypes are all no-calls now (it's not possible to fix them easily)
     * Alleles are right trimmed to satisfy VCF conventions
     *
     * If vc is biallelic or non-variant it is just returned
     *
     * Chromosome counts are updated (but they are by definition 0)
     *
     * @param vc a potentially multi-allelic variant context
     * @param trimLeft if true, we will also left trim alleles, potentially moving the resulting vcs forward on the genome
     * @return a list of bi-allelic (or monomorphic) variant context
     */
    public static List<VariantContext> splitVariantContextToBiallelics(final VariantContext vc, final boolean trimLeft, final GenotypeAssignmentMethod genotypeAssignmentMethod) {
        if ( ! vc.isVariant() || vc.isBiallelic() )
            // non variant or biallelics already satisfy the contract
            return Collections.singletonList(vc);
        else {
            final List<VariantContext> biallelics = new LinkedList<>();

            for ( final Allele alt : vc.getAlternateAlleles() ) {
                VariantContextBuilder builder = new VariantContextBuilder(vc);
                final List<Allele> alleles = Arrays.asList(vc.getReference(), alt);
                builder.alleles(alleles);
                builder.genotypes(subsetDiploidAlleles(vc, alleles, genotypeAssignmentMethod));
                VariantContextUtils.calculateChromosomeCounts(builder, true);
                final VariantContext trimmed = trimAlleles(builder.make(), trimLeft, true);
                biallelics.add(trimmed);
            }

            return biallelics;
        }
    }

    public static Genotype removePLsAndAD(final Genotype g) {
        return ( g.hasLikelihoods() || g.hasAD() ) ? new GenotypeBuilder(g).noPL().noAD().make() : g;
    }

    /**
     * Merges VariantContexts into a single hybrid.  Takes genotypes for common samples in priority order, if provided.
     * If uniquifySamples is true, the priority order is ignored and names are created by concatenating the VC name with
     * the sample name
     *
     * @param unsortedVCs               collection of unsorted VCs
     * @param priorityListOfVCs         priority list detailing the order in which we should grab the VCs
     * @param filteredRecordMergeType   merge type for filtered records
     * @param genotypeMergeOptions      merge option for genotypes
     * @param annotateOrigin            should we annotate the set it came from?
     * @param printMessages             should we print messages?
     * @param setKey                    the key name of the set
     * @param filteredAreUncalled       are filtered records uncalled?
     * @param mergeInfoWithMaxAC        should we merge in info from the VC with maximum allele count?
     * @return new VariantContext       representing the merge of unsortedVCs
     */
    public static VariantContext simpleMerge(final Collection<VariantContext> unsortedVCs,
                                             final List<String> priorityListOfVCs,
                                             final FilteredRecordMergeType filteredRecordMergeType,
                                             final GenotypeMergeType genotypeMergeOptions,
                                             final boolean annotateOrigin,
                                             final boolean printMessages,
                                             final String setKey,
                                             final boolean filteredAreUncalled,
                                             final boolean mergeInfoWithMaxAC ) {
        int originalNumOfVCs = priorityListOfVCs == null ? 0 : priorityListOfVCs.size();
        return simpleMerge(unsortedVCs, priorityListOfVCs, originalNumOfVCs, filteredRecordMergeType, genotypeMergeOptions, annotateOrigin, printMessages, setKey, filteredAreUncalled, mergeInfoWithMaxAC);
    }

    /**
     * Merges VariantContexts into a single hybrid.  Takes genotypes for common samples in priority order, if provided.
     * If uniquifySamples is true, the priority order is ignored and names are created by concatenating the VC name with
     * the sample name.
     * simpleMerge does not verify any more unique sample names EVEN if genotypeMergeOptions == GenotypeMergeType.REQUIRE_UNIQUE. One should use
     * SampleUtils.verifyUniqueSamplesNames to check that before using simpleMerge.
     *
     * For more information on this method see: http://www.thedistractionnetwork.com/programmer-problem/
     *
     * @param unsortedVCs               collection of unsorted VCs
     * @param priorityListOfVCs         priority list detailing the order in which we should grab the VCs
     * @param filteredRecordMergeType   merge type for filtered records
     * @param genotypeMergeOptions      merge option for genotypes
     * @param annotateOrigin            should we annotate the set it came from?
     * @param printMessages             should we print messages?
     * @param setKey                    the key name of the set
     * @param filteredAreUncalled       are filtered records uncalled?
     * @param mergeInfoWithMaxAC        should we merge in info from the VC with maximum allele count?
     * @return new VariantContext       representing the merge of unsortedVCs
     */
    public static VariantContext simpleMerge(final Collection<VariantContext> unsortedVCs,
                                             final List<String> priorityListOfVCs,
                                             final int originalNumOfVCs,
                                             final FilteredRecordMergeType filteredRecordMergeType,
                                             final GenotypeMergeType genotypeMergeOptions,
                                             final boolean annotateOrigin,
                                             final boolean printMessages,
                                             final String setKey,
                                             final boolean filteredAreUncalled,
                                             final boolean mergeInfoWithMaxAC ) {
        if ( unsortedVCs == null || unsortedVCs.size() == 0 )
            return null;

        if (priorityListOfVCs != null && originalNumOfVCs != priorityListOfVCs.size())
            throw new IllegalArgumentException("the number of the original VariantContexts must be the same as the number of VariantContexts in the priority list");

        if ( annotateOrigin && priorityListOfVCs == null && originalNumOfVCs == 0)
            throw new IllegalArgumentException("Cannot merge calls and annotate their origins without a complete priority list of VariantContexts or the number of original VariantContexts");

        final List<VariantContext> preFilteredVCs = sortVariantContextsByPriority(unsortedVCs, priorityListOfVCs, genotypeMergeOptions);
        // Make sure all variant contexts are padded with reference base in case of indels if necessary
        List<VariantContext> VCs = new ArrayList<>();

        for (final VariantContext vc : preFilteredVCs) {
            if ( ! filteredAreUncalled || vc.isNotFiltered() )
                VCs.add(vc);
        }

        if ( VCs.size() == 0 ) // everything is filtered out and we're filteredAreUncalled
            return null;

        // establish the baseline info from the first VC
        final VariantContext first = VCs.get(0);
        final String name = first.getSource();
        final Allele refAllele = determineReferenceAllele(VCs);

        final Set<Allele> alleles = new LinkedHashSet<>();
        final Set<String> filters = new HashSet<>();
        final Map<String, Object> attributes = new LinkedHashMap<>();
        final Set<String> inconsistentAttributes = new HashSet<>();
        final Set<String> variantSources = new HashSet<>(); // contains the set of sources we found in our set of VCs that are variant
        final Set<String> rsIDs = new LinkedHashSet<>(1); // most of the time there's one id

        VariantContext longestVC = first;
        int depth = 0;
        int maxAC = -1;
        final Map<String, Object> attributesWithMaxAC = new LinkedHashMap<>();
        final Map<String, List<Comparable>> annotationMap = new LinkedHashMap<>();
        double log10PError = CommonInfo.NO_LOG10_PERROR;
        boolean anyVCHadFiltersApplied = false;
        VariantContext vcWithMaxAC = null;
        GenotypesContext genotypes = GenotypesContext.create();

        // counting the number of filtered and variant VCs
        int nFiltered = 0;

        boolean remapped = false;

        // cycle through and add info from the other VCs, making sure the loc/reference matches
        for ( final VariantContext vc : VCs ) {
            if ( longestVC.getStart() != vc.getStart() )
                throw new IllegalStateException("BUG: attempting to merge VariantContexts with different start sites: first="+ first.toString() + " second=" + vc.toString());

            if ( VariantContextUtils.getSize(vc) > VariantContextUtils.getSize(longestVC) )
                longestVC = vc; // get the longest location

            nFiltered += vc.isFiltered() ? 1 : 0;
            if ( vc.isVariant() ) variantSources.add(vc.getSource());

            AlleleMapper alleleMapping = resolveIncompatibleAlleles(refAllele, vc, alleles);
            remapped = remapped || alleleMapping.needsRemapping();

            alleles.addAll(alleleMapping.values());

            mergeGenotypes(genotypes, vc, alleleMapping, genotypeMergeOptions == GenotypeMergeType.UNIQUIFY);

            // We always take the QUAL of the first VC with a non-MISSING qual for the combined value
            if ( log10PError == CommonInfo.NO_LOG10_PERROR )
                log10PError =  vc.getLog10PError();

            filters.addAll(vc.getFilters());
            anyVCHadFiltersApplied |= vc.filtersWereApplied();

            //
            // add attributes
            //
            // special case DP (add it up) and ID (just preserve it)
            //
            if (vc.hasAttribute(VCFConstants.DEPTH_KEY))
                depth += vc.getAttributeAsInt(VCFConstants.DEPTH_KEY, 0);
            if ( vc.hasID() ) rsIDs.add(vc.getID());
            if (mergeInfoWithMaxAC && vc.hasAttribute(VCFConstants.ALLELE_COUNT_KEY)) {
                String rawAlleleCounts = vc.getAttributeAsString(VCFConstants.ALLELE_COUNT_KEY, null);
                // lets see if the string contains a "," separator
                if (rawAlleleCounts.contains(VCFConstants.INFO_FIELD_ARRAY_SEPARATOR)) {
                    final List<String> alleleCountArray = Arrays.asList(rawAlleleCounts.substring(1, rawAlleleCounts.length() - 1).split(VCFConstants.INFO_FIELD_ARRAY_SEPARATOR));
                    for (final String alleleCount : alleleCountArray) {
                        final int ac = Integer.valueOf(alleleCount.trim());
                        if (ac > maxAC) {
                            maxAC = ac;
                            vcWithMaxAC = vc;
                        }
                    }
                } else {
                    final int ac = Integer.valueOf(rawAlleleCounts);
                    if (ac > maxAC) {
                        maxAC = ac;
                        vcWithMaxAC = vc;
                    }
                }
            }

            for (final Map.Entry<String, Object> p : vc.getAttributes().entrySet()) {
                final String key = p.getKey();
                final Object value = p.getValue();
                // only output annotations that have the same value in every input VC
                // if we don't like the key already, don't go anywhere
                if ( ! inconsistentAttributes.contains(key) ) {
                    final boolean alreadyFound = attributes.containsKey(key);
                    final Object boundValue = attributes.get(key);
                    final boolean boundIsMissingValue = alreadyFound && boundValue.equals(VCFConstants.MISSING_VALUE_v4);

                    if ( alreadyFound && ! boundValue.equals(value) && ! boundIsMissingValue ) {
                        // we found the value but we're inconsistent, put it in the exclude list
                        inconsistentAttributes.add(key);
                        attributes.remove(key);
                    } else if ( ! alreadyFound || boundIsMissingValue )  { // no value
                        attributes.put(key, value);
                    }
                }
            }
        }

        // if we have more alternate alleles in the merged VC than in one or more of the
        // original VCs, we need to strip out the GL/PLs (because they are no longer accurate), as well as allele-dependent attributes like AC,AF, and AD
        for ( final VariantContext vc : VCs ) {
            if (vc.getAlleles().size() == 1)
                continue;
            if ( hasPLIncompatibleAlleles(alleles, vc.getAlleles())) {
                if ( ! genotypes.isEmpty() ) {
                    logger.debug(String.format("Stripping PLs at %s:%d-%d due to incompatible alleles merged=%s vs. single=%s",
                            vc.getChr(), vc.getStart(), vc.getEnd(), alleles, vc.getAlleles()));
                }
                genotypes = stripPLsAndAD(genotypes);
                // this will remove stale AC,AF attributed from vc
                VariantContextUtils.calculateChromosomeCounts(vc, attributes, true);
                break;
            }
        }

        // take the VC with the maxAC and pull the attributes into a modifiable map
        if ( mergeInfoWithMaxAC && vcWithMaxAC != null ) {
            attributesWithMaxAC.putAll(vcWithMaxAC.getAttributes());
        }

        // if at least one record was unfiltered and we want a union, clear all of the filters
        if ( (filteredRecordMergeType == FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED && nFiltered != VCs.size()) || filteredRecordMergeType == FilteredRecordMergeType.KEEP_UNCONDITIONAL )
            filters.clear();


        if ( annotateOrigin ) { // we care about where the call came from
            String setValue;
            if ( nFiltered == 0 && variantSources.size() == originalNumOfVCs ) // nothing was unfiltered
                setValue = MERGE_INTERSECTION;
            else if ( nFiltered == VCs.size() )     // everything was filtered out
                setValue = MERGE_FILTER_IN_ALL;
            else if ( variantSources.isEmpty() )    // everyone was reference
                setValue = MERGE_REF_IN_ALL;
            else {
                final LinkedHashSet<String> s = new LinkedHashSet<>();
                for ( final VariantContext vc : VCs )
                    if ( vc.isVariant() )
                        s.add( vc.isFiltered() ? MERGE_FILTER_PREFIX + vc.getSource() : vc.getSource() );
                setValue = Utils.join("-", s);
            }

            if ( setKey != null ) {
                attributes.put(setKey, setValue);
                if( mergeInfoWithMaxAC && vcWithMaxAC != null ) {
                    attributesWithMaxAC.put(setKey, setValue);
                }
            }
        }

        if ( depth > 0 )
            attributes.put(VCFConstants.DEPTH_KEY, String.valueOf(depth));

        final String ID = rsIDs.isEmpty() ? VCFConstants.EMPTY_ID_FIELD : Utils.join(",", rsIDs);

        final VariantContextBuilder builder = new VariantContextBuilder().source(name).id(ID);
        builder.loc(longestVC.getChr(), longestVC.getStart(), longestVC.getEnd());
        builder.alleles(alleles);
        builder.genotypes(genotypes);
        builder.log10PError(log10PError);
        if ( anyVCHadFiltersApplied ) {
            builder.filters(filters.isEmpty() ? filters : new TreeSet<>(filters));
        }
        builder.attributes(new TreeMap<>(mergeInfoWithMaxAC ? attributesWithMaxAC : attributes));

        // Trim the padded bases of all alleles if necessary
        final VariantContext merged = builder.make();
        if ( printMessages && remapped ) System.out.printf("Remapped => %s%n", merged);
        return merged;
    }

    private static Comparable combineAnnotationValues( final List<Comparable> array ) {
        return MathUtils.median(array); // right now we take the median but other options could be explored
    }

    /**
     * Merges VariantContexts from gVCFs into a single hybrid.
     * Assumes that none of the input records are filtered.
     *
     * @param VCs     collection of unsorted genomic VCs
     * @param loc     the current location
     * @param refBase the reference allele to use if all contexts in the VC are spanning (i.e. don't start at the location in loc); if null, we'll return null in this case
     * @return new VariantContext representing the merge of all VCs or null if it not relevant
     */
    public static VariantContext referenceConfidenceMerge(final List<VariantContext> VCs, final GenomeLoc loc, final Byte refBase) {
        // this can happen if e.g. you are using a dbSNP file that spans a region with no gVCFs
        if ( VCs == null || VCs.size() == 0 )
            return null;

        // establish the baseline info (sometimes from the first VC)
        final VariantContext first = VCs.get(0);
        final String name = first.getSource();

        // ref allele
        final Allele refAllele = determineReferenceAlleleGiveReferenceBase(VCs, loc, refBase);
        if ( refAllele == null )
            return null;

        // alt alleles
        final AlleleMapper alleleMapper = determineAlternateAlleleMapping(VCs, refAllele, loc);
        final List<Allele> alleles = getAllelesListFromMapper(refAllele, alleleMapper);

        final Map<String, Object> attributes = new LinkedHashMap<>();
        final Set<String> inconsistentAttributes = new HashSet<>();
        final Set<String> rsIDs = new LinkedHashSet<>(1); // most of the time there's one id

        int depth = 0;
        final Map<String, List<Comparable>> annotationMap = new LinkedHashMap<>();
        GenotypesContext genotypes = GenotypesContext.create();

        // cycle through and add info from the other VCs
        for ( final VariantContext vc : VCs ) {

            // if this context doesn't start at the current location then it must be a spanning event (deletion or ref block)
            final boolean isSpanningEvent = loc.getStart() != vc.getStart();
            final List<Allele> remappedAlleles = isSpanningEvent ? replaceWithNoCalls(vc.getAlleles()) : alleleMapper.remap(vc.getAlleles());
            mergeRefConfidenceGenotypes(genotypes, vc, remappedAlleles, alleles);

            // special case DP (add it up) for all events
            if ( vc.hasAttribute(VCFConstants.DEPTH_KEY) )
                depth += vc.getAttributeAsInt(VCFConstants.DEPTH_KEY, 0);

            if ( isSpanningEvent )
                continue;

            // special case ID (just preserve it)
            if ( vc.hasID() ) rsIDs.add(vc.getID());

            // add attributes
            addReferenceConfidenceAttributes(vc.getAttributes(), attributes, inconsistentAttributes, annotationMap);
        }

        // when combining annotations use the median value from all input VCs which had annotations provided
        for ( final Map.Entry<String, List<Comparable>> p : annotationMap.entrySet() ) {
            if ( ! p.getValue().isEmpty() ) {
                attributes.put(p.getKey(), combineAnnotationValues(p.getValue()));
            }
        }

        if ( depth > 0 )
            attributes.put(VCFConstants.DEPTH_KEY, String.valueOf(depth));

        // remove stale AC and AF based attributes
        removeStaleAttributesAfterMerge(attributes);

        final String ID = rsIDs.isEmpty() ? VCFConstants.EMPTY_ID_FIELD : Utils.join(",", rsIDs);

        final VariantContextBuilder builder = new VariantContextBuilder().source(name).id(ID).alleles(alleles)
                .chr(loc.getContig()).start(loc.getStart()).computeEndFromAlleles(alleles, loc.getStart())
                .genotypes(genotypes).unfiltered().attributes(new TreeMap<>(attributes)).log10PError(CommonInfo.NO_LOG10_PERROR);  // we will need to regenotype later

        return builder.make();
    }

    /**
     * Determines the ref allele given the provided reference base at this position
     *
     * @param VCs     collection of unsorted genomic VCs
     * @param loc     the current location
     * @param refBase the reference allele to use if all contexts in the VC are spanning
     * @return new Allele or null if no reference allele/base is available
     */
    private static Allele determineReferenceAlleleGiveReferenceBase(final List<VariantContext> VCs, final GenomeLoc loc, final Byte refBase) {
        final Allele refAllele = determineReferenceAllele(VCs, loc);
        if ( refAllele == null )
            return ( refBase == null ? null : Allele.create(refBase, true) );
        return refAllele;
    }

    /**
     * Creates an alleles list given a reference allele and a mapper
     *
     * @param refAllele      the reference allele
     * @param alleleMapper   the allele mapper
     * @return a non-null, non-empty list of Alleles
     */
    private static List<Allele> getAllelesListFromMapper(final Allele refAllele, final AlleleMapper alleleMapper) {
        final List<Allele> alleles = new ArrayList<>();
        alleles.add(refAllele);
        alleles.addAll(alleleMapper.getUniqueMappedAlleles());
        return alleles;
    }

    /**
     * Remove the stale attributes from the merged set
     *
     * @param attributes the attribute map
     */
    private static void removeStaleAttributesAfterMerge(final Map<String, Object> attributes) {
        attributes.remove(VCFConstants.ALLELE_COUNT_KEY);
        attributes.remove(VCFConstants.ALLELE_FREQUENCY_KEY);
        attributes.remove(VCFConstants.ALLELE_NUMBER_KEY);
        attributes.remove(VCFConstants.MLE_ALLELE_COUNT_KEY);
        attributes.remove(VCFConstants.MLE_ALLELE_FREQUENCY_KEY);
        attributes.remove(VCFConstants.END_KEY);
    }

    /**
     * Adds attributes to the global map from the new context in a sophisticated manner
     *
     * @param myAttributes               attributes to add from
     * @param globalAttributes           global set of attributes to add to
     * @param inconsistentAttributes     set of attributes that are inconsistent among samples
     * @param annotationMap              map of annotations for combining later
     */
    private static void addReferenceConfidenceAttributes(final Map<String, Object> myAttributes,
                                                         final Map<String, Object> globalAttributes,
                                                         final Set<String> inconsistentAttributes,
                                                         final Map<String, List<Comparable>> annotationMap) {
        for ( final Map.Entry<String, Object> p : myAttributes.entrySet() ) {
            final String key = p.getKey();
            final Object value = p.getValue();
            boolean badAnnotation = false;

            // add the annotation values to a list for combining later
            List<Comparable> values = annotationMap.get(key);
            if( values == null ) {
                values = new ArrayList<>();
                annotationMap.put(key, values);
            }
            try {
                final String stringValue = value.toString();
                // Branch to avoid unintentional, implicit type conversions that occur with the ? operator.
                if (stringValue.contains("."))
                    values.add(Double.parseDouble(stringValue));
                else
                    values.add(Integer.parseInt(stringValue));
            } catch (final NumberFormatException e) {
                badAnnotation = true;
            }

            // only output annotations that have the same value in every input VC
            if ( badAnnotation && ! inconsistentAttributes.contains(key) ) {
                checkForConsistency(key, value, globalAttributes, inconsistentAttributes);
            }
        }
    }

    /**
     * Check attributes for consistency to others in the merge
     *
     * @param key                        the attribute key
     * @param value                      the attribute value
     * @param globalAttributes           the global list of attributes being merged
     * @param inconsistentAttributes     the list of inconsistent attributes in the merge
     */
    private static void checkForConsistency(final String key,
                                            final Object value,
                                            final Map<String, Object> globalAttributes,
                                            final Set<String> inconsistentAttributes) {
        final boolean alreadyFound = globalAttributes.containsKey(key);
        final Object boundValue = globalAttributes.get(key);
        final boolean boundIsMissingValue = alreadyFound && boundValue.equals(VCFConstants.MISSING_VALUE_v4);

        if ( alreadyFound && ! boundValue.equals(value) && ! boundIsMissingValue ) {
            // we found the value but we're inconsistent, put it in the exclude list
            inconsistentAttributes.add(key);
            globalAttributes.remove(key);
        } else if ( ! alreadyFound || boundIsMissingValue )  { // no value
            globalAttributes.put(key, value);
        }
    }

    private static boolean hasPLIncompatibleAlleles(final Collection<Allele> alleleSet1, final Collection<Allele> alleleSet2) {
        final Iterator<Allele> it1 = alleleSet1.iterator();
        final Iterator<Allele> it2 = alleleSet2.iterator();

        while ( it1.hasNext() && it2.hasNext() ) {
            final Allele a1 = it1.next();
            final Allele a2 = it2.next();
            if ( ! a1.equals(a2) )
                return true;
        }

        // by this point, at least one of the iterators is empty.  All of the elements
        // we've compared are equal up until this point.  But it's possible that the
        // sets aren't the same size, which is indicated by the test below.  If they
        // are of the same size, though, the sets are compatible
        return it1.hasNext() || it2.hasNext();
    }

    public static GenotypesContext stripPLsAndAD(GenotypesContext genotypes) {
        final GenotypesContext newGs = GenotypesContext.create(genotypes.size());

        for ( final Genotype g : genotypes ) {
            newGs.add(removePLsAndAD(g));
        }

        return newGs;
    }

    /**
     * Updates the PLs and AD of the Genotypes in the newly selected VariantContext to reflect the fact that some alleles
     * from the original VariantContext are no longer present.
     *
     * @param selectedVC  the selected (new) VariantContext
     * @param originalVC  the original VariantContext
     * @return a new non-null GenotypesContext
     */
    public static GenotypesContext updatePLsAndAD(final VariantContext selectedVC, final VariantContext originalVC) {
        final int numNewAlleles = selectedVC.getAlleles().size();
        final int numOriginalAlleles = originalVC.getAlleles().size();

        // if we have more alternate alleles in the selected VC than in the original VC, then something is wrong
        if ( numNewAlleles > numOriginalAlleles )
            throw new IllegalArgumentException("Attempting to fix PLs and AD from what appears to be a *combined* VCF and not a selected one");

        final GenotypesContext oldGs = selectedVC.getGenotypes();

        // if we have the same number of alternate alleles in the selected VC as in the original VC, then we don't need to fix anything
        if ( numNewAlleles == numOriginalAlleles )
            return oldGs;

        final GenotypesContext newGs = fixPLsFromSubsettedAlleles(oldGs, originalVC, selectedVC.getAlleles());

        return fixADFromSubsettedAlleles(newGs, originalVC, selectedVC.getAlleles());
    }

    /**
     * Fix the PLs for the GenotypesContext of a VariantContext that has been subset
     *
     * @param originalGs       the original GenotypesContext
     * @param originalVC       the original VariantContext
     * @param allelesToUse     the new (sub)set of alleles to use
     * @return a new non-null GenotypesContext
     */
    static private GenotypesContext fixPLsFromSubsettedAlleles(final GenotypesContext originalGs, final VariantContext originalVC, final List<Allele> allelesToUse) {

        // we need to determine which of the alternate alleles (and hence the likelihoods) to use and carry forward
        final List<Integer> likelihoodIndexesToUse = determineLikelihoodIndexesToUse(originalVC, allelesToUse);

        // create the new genotypes
        return createGenotypesWithSubsettedLikelihoods(originalGs, originalVC, allelesToUse, likelihoodIndexesToUse, GenotypeAssignmentMethod.DO_NOT_ASSIGN_GENOTYPES);
    }

    /**
     * Fix the AD for the GenotypesContext of a VariantContext that has been subset
     *
     * @param originalGs       the original GenotypesContext
     * @param originalVC       the original VariantContext
     * @param allelesToUse     the new (sub)set of alleles to use
     * @return a new non-null GenotypesContext
     */
    static private GenotypesContext fixADFromSubsettedAlleles(final GenotypesContext originalGs, final VariantContext originalVC, final List<Allele> allelesToUse) {

        // the bitset representing the allele indexes we want to keep
        final boolean[] alleleIndexesToUse = getAlleleIndexBitset(originalVC, allelesToUse);

        // the new genotypes to create
        final GenotypesContext newGTs = GenotypesContext.create(originalGs.size());

        // the samples
        final List<String> sampleIndices = originalGs.getSampleNamesOrderedByName();

        // create the new genotypes
        for ( int k = 0; k < originalGs.size(); k++ ) {
            final Genotype g = originalGs.get(sampleIndices.get(k));
            newGTs.add(fixAD(g, alleleIndexesToUse, allelesToUse.size()));
        }

        return newGTs;
    }

    /**
     * Fix the AD for the given Genotype
     *
     * @param genotype              the original Genotype
     * @param alleleIndexesToUse    a bitset describing whether or not to keep a given index
     * @param nAllelesToUse         how many alleles we are keeping
     * @return a non-null Genotype
     */
    private static Genotype fixAD(final Genotype genotype, final boolean[] alleleIndexesToUse, final int nAllelesToUse) {
        // if it ain't broke don't fix it
        if ( !genotype.hasAD() )
            return genotype;

        final GenotypeBuilder builder = new GenotypeBuilder(genotype);

        final int[] oldAD = genotype.getAD();
        if ( oldAD.length != alleleIndexesToUse.length ) {
            builder.noAD();
        } else {
            final int[] newAD = new int[nAllelesToUse];
            int currentIndex = 0;
            for ( int i = 0; i < oldAD.length; i++ ) {
                if ( alleleIndexesToUse[i] )
                    newAD[currentIndex++] = oldAD[i];
            }
            builder.AD(newAD);
        }
        return builder.make();
    }

    static private Allele determineReferenceAllele(final List<VariantContext> VCs) {
        return determineReferenceAllele(VCs, null);
    }

    /**
     * Determines the common reference allele
     *
     * @param VCs    the list of VariantContexts
     * @param loc    if not null, ignore records that do not begin at this start location
     * @return possibly null Allele
     */
    static private Allele determineReferenceAllele(final List<VariantContext> VCs, final GenomeLoc loc) {
        Allele ref = null;

        for ( final VariantContext vc : VCs ) {
            if ( contextMatchesLoc(vc, loc) ) {
                final Allele myRef = vc.getReference();
                if ( ref == null || ref.length() < myRef.length() )
                    ref = myRef;
                else if ( ref.length() == myRef.length() && ! ref.equals(myRef) )
                    throw new TribbleException(String.format("The provided variant file(s) have inconsistent references for the same position(s) at %s:%d, %s vs. %s", vc.getChr(), vc.getStart(), ref, myRef));
            }
        }

        return ref;
    }

    static private boolean contextMatchesLoc(final VariantContext vc, final GenomeLoc loc) {
        return loc == null || loc.getStart() == vc.getStart();
    }

    /**
     * Given the reference allele, determines the mapping for common alternate alleles in the list of VariantContexts.
     *
     * @param VCs        the list of VariantContexts
     * @param refAllele  the reference allele
     * @param loc        if not null, ignore records that do not begin at this start location
     * @return non-null AlleleMapper
     */
    static private AlleleMapper determineAlternateAlleleMapping(final List<VariantContext> VCs, final Allele refAllele, final GenomeLoc loc) {
        final Map<Allele, Allele> map = new HashMap<>();

        for ( final VariantContext vc : VCs ) {
            if ( contextMatchesLoc(vc, loc) )
                addAllAlternateAllelesToMap(vc, refAllele, map);
        }

        return new AlleleMapper(map);
    }

    /**
     * Adds all of the alternate alleles from the VariantContext to the allele mapping (for use in creating the AlleleMapper)
     *
     * @param vc           the VariantContext
     * @param refAllele    the reference allele
     * @param map          the allele mapping to populate
     */
    static private void addAllAlternateAllelesToMap(final VariantContext vc, final Allele refAllele, final Map<Allele, Allele> map) {
        // if the ref allele matches, then just add the alts as is
        if ( refAllele.equals(vc.getReference()) ) {
            for ( final Allele altAllele : vc.getAlternateAlleles() ) {
                // ignore symbolic alleles
                if ( ! altAllele.isSymbolic() )
                    map.put(altAllele, altAllele);
            }
        }
        else {
            map.putAll(createAlleleMapping(refAllele, vc, map.values()));
        }
    }

    static private AlleleMapper resolveIncompatibleAlleles(final Allele refAllele, final VariantContext vc, final Set<Allele> allAlleles) {
        if ( refAllele.equals(vc.getReference()) )
            return new AlleleMapper(vc);
        else {
            final Map<Allele, Allele> map = createAlleleMapping(refAllele, vc, allAlleles);
            map.put(vc.getReference(), refAllele);
            return new AlleleMapper(map);
        }
    }

    /**
     * Create an allele mapping for the given context where its reference allele must (potentially) be extended to the given allele
     *
     * The refAllele is the longest reference allele seen at this start site.
     * So imagine it is:
     * refAllele: ACGTGA
     * myRef:     ACGT
     * myAlt:     A
     *
     * We need to remap all of the alleles in vc to include the extra GA so that
     * myRef => refAllele and myAlt => AGA
     *
     * @param refAllele          the new (extended) reference allele
     * @param oneVC              the Variant Context to extend
     * @param currentAlleles     the list of alleles already created
     * @return a non-null mapping of original alleles to new (extended) ones
     */
    private static Map<Allele, Allele> createAlleleMapping(final Allele refAllele,
                                                           final VariantContext oneVC,
                                                           final Collection<Allele> currentAlleles) {
        final Allele myRef = oneVC.getReference();
        if ( refAllele.length() <= myRef.length() ) throw new IllegalStateException("BUG: myRef="+myRef+" is longer than refAllele="+refAllele);

        final byte[] extraBases = Arrays.copyOfRange(refAllele.getBases(), myRef.length(), refAllele.length());

        final Map<Allele, Allele> map = new HashMap<>();
        for ( final Allele a : oneVC.getAlternateAlleles() ) {
            if ( isUsableAlternateAllele(a) ) {
                Allele extended = Allele.extend(a, extraBases);
                for ( final Allele b : currentAlleles )
                    if ( extended.equals(b) )
                        extended = b;
                map.put(a, extended);
            }
        }

        return map;
    }

    static private boolean isUsableAlternateAllele(final Allele allele) {
        return ! (allele.isReference() || allele.isSymbolic() );
    }

    public static List<VariantContext> sortVariantContextsByPriority(Collection<VariantContext> unsortedVCs, List<String> priorityListOfVCs, GenotypeMergeType mergeOption ) {
        if ( mergeOption == GenotypeMergeType.PRIORITIZE && priorityListOfVCs == null )
            throw new IllegalArgumentException("Cannot merge calls by priority with a null priority list");

        if ( priorityListOfVCs == null || mergeOption == GenotypeMergeType.UNSORTED )
            return new ArrayList<>(unsortedVCs);
        else {
            ArrayList<VariantContext> sorted = new ArrayList<>(unsortedVCs);
            Collections.sort(sorted, new CompareByPriority(priorityListOfVCs));
            return sorted;
        }
    }

    private static void mergeGenotypes(GenotypesContext mergedGenotypes, VariantContext oneVC, AlleleMapper alleleMapping, boolean uniquifySamples) {
        //TODO: should we add a check for cases when the genotypeMergeOption is REQUIRE_UNIQUE
        for ( final Genotype g : oneVC.getGenotypes() ) {
            final String name = mergedSampleName(oneVC.getSource(), g.getSampleName(), uniquifySamples);
            if ( ! mergedGenotypes.containsSample(name) ) {
                // only add if the name is new
                Genotype newG = g;

                if ( uniquifySamples || alleleMapping.needsRemapping() ) {
                    final List<Allele> alleles = alleleMapping.needsRemapping() ? alleleMapping.remap(g.getAlleles()) : g.getAlleles();
                    newG = new GenotypeBuilder(g).name(name).alleles(alleles).make();
                }

                mergedGenotypes.add(newG);
            }
        }
    }

    /**
     * Replaces any alleles in the list with NO CALLS, except for the generic ALT allele
     *
     * @param alleles list of alleles to replace
     * @return non-null list of alleles
     */
    private static List<Allele> replaceWithNoCalls(final List<Allele> alleles) {
        if ( alleles == null ) throw new IllegalArgumentException("list of alleles cannot be null");

        final List<Allele> result = new ArrayList<>(alleles.size());
        for ( final Allele allele : alleles )
            result.add(allele.equals(NON_REF_SYMBOLIC_ALLELE) ? allele : Allele.NO_CALL);
        return result;
    }

    /**
     * Merge into the context a new genotype represented by the given VariantContext for the provided list of target alleles.
     * This method assumes that none of the alleles in the VC overlaps with any of the alleles in the set.
     *
     * @param mergedGenotypes   the genotypes context to add to
     * @param VC                the Variant Context for the sample
     * @param remappedAlleles   the list of remapped alleles for the sample
     * @param targetAlleles     the list of target alleles
     */
    private static void mergeRefConfidenceGenotypes(final GenotypesContext mergedGenotypes,
                                                    final VariantContext VC,
                                                    final List<Allele> remappedAlleles,
                                                    final List<Allele> targetAlleles) {
        for ( final Genotype g : VC.getGenotypes() ) {
	    if ( !g.hasPL() )
		throw new UserException("cannot merge genotypes from samples without PLs; sample " + g.getSampleName() + " does not have likelihoods at position " + VC.getChr() + ":" + VC.getStart());

            // only add if the name is new
            final String name = g.getSampleName();
            if ( !mergedGenotypes.containsSample(name) ) {
                // we need to modify it even if it already contains all of the alleles because we need to purge the <ALT> PLs out anyways
                final int[] indexesOfRelevantAlleles = getIndexesOfRelevantAlleles(remappedAlleles, targetAlleles, VC.getStart());
                final int[] PLs = generatePLs(g, indexesOfRelevantAlleles);
                // note that we set the alleles to null here (as we expect it to be re-genotyped)
                final Genotype newG = new GenotypeBuilder(g).name(name).alleles(null).PL(PLs).noAD().noGQ().make();
                mergedGenotypes.add(newG);
            }
        }
    }

    /**
     * Determines the allele mapping from myAlleles to the targetAlleles, substituting the generic "<ALT>" as appropriate.
     * If the myAlleles set does not contain "<ALT>" as an allele, it throws an exception.
     *
     * @param remappedAlleles   the list of alleles to evaluate
     * @param targetAlleles     the target list of alleles
     * @param position          position to use for error messages
     * @return non-null array of ints representing indexes
     */
    protected static int[] getIndexesOfRelevantAlleles(final List<Allele> remappedAlleles, final List<Allele> targetAlleles, final int position) {

        if ( remappedAlleles == null || remappedAlleles.size() == 0 ) throw new IllegalArgumentException("The list of input alleles must not be null or empty");
        if ( targetAlleles == null || targetAlleles.size() == 0 ) throw new IllegalArgumentException("The list of target alleles must not be null or empty");

        if ( !remappedAlleles.contains(NON_REF_SYMBOLIC_ALLELE) )
            throw new UserException("The list of input alleles must contain " + NON_REF_SYMBOLIC_ALLELE + " as an allele but that is not the case at position " + position + "; please use the Haplotype Caller with gVCF output to generate appropriate records");
        final int indexOfGenericAlt = remappedAlleles.indexOf(NON_REF_SYMBOLIC_ALLELE);

        final int[] indexMapping = new int[targetAlleles.size()];

        // the reference alleles always match up (even if they don't appear to)
        indexMapping[0] = 0;

        // create the index mapping, using the <ALT> allele whenever such a mapping doesn't exist
        for ( int i = 1; i < targetAlleles.size(); i++ ) {
            final int indexOfRemappedAllele = remappedAlleles.indexOf(targetAlleles.get(i));
            indexMapping[i] = indexOfRemappedAllele == -1 ? indexOfGenericAlt: indexOfRemappedAllele;
        }

        return indexMapping;
    }

    /**
     * Generates new PLs given the set of indexes of the Genotype's current alleles from the original PLs.
     * Throws an exception if the Genotype does not contain PLs.
     *
     * @param genotype    the genotype from which to grab PLs
     * @param indexesOfRelevantAlleles the indexes of the original alleles corresponding to the new alleles
     * @return non-null array of new PLs
     */
    protected static int[] generatePLs(final Genotype genotype, final int[] indexesOfRelevantAlleles) {
        if ( !genotype.hasPL() )
            throw new IllegalArgumentException("Cannot generate new PLs from a genotype without PLs");

        final int[] originalPLs = genotype.getPL();

        // assume diploid
        final int numLikelihoods = GenotypeLikelihoods.numLikelihoods(indexesOfRelevantAlleles.length, 2);
        final int[] newPLs = new int[numLikelihoods];

        for ( int i = 0; i < indexesOfRelevantAlleles.length; i++ ) {
            for ( int j = i; j < indexesOfRelevantAlleles.length; j++ ) {
                final int originalPLindex = calculatePLindexFromUnorderedIndexes(indexesOfRelevantAlleles[i], indexesOfRelevantAlleles[j]);
                if ( originalPLindex >= originalPLs.length )
                    throw new IllegalStateException("The original PLs do not have enough values; accessing index " + originalPLindex + " but size is " + originalPLs.length);

                final int newPLindex = GenotypeLikelihoods.calculatePLindex(i, j);
                newPLs[newPLindex] = originalPLs[originalPLindex];
            }
        }

        return newPLs;
    }

    /**
     * This is just a safe wrapper around GenotypeLikelihoods.calculatePLindex()
     *
     * @param originalIndex1   the index of the first allele
     * @param originalIndex2   the index of the second allele
     * @return the PL index
     */
    protected static int calculatePLindexFromUnorderedIndexes(final int originalIndex1, final int originalIndex2) {
        // we need to make sure they are ordered correctly
        return ( originalIndex2 < originalIndex1 ) ? GenotypeLikelihoods.calculatePLindex(originalIndex2, originalIndex1) : GenotypeLikelihoods.calculatePLindex(originalIndex1, originalIndex2);
    }

    public static String mergedSampleName(String trackName, String sampleName, boolean uniquify ) {
        return uniquify ? sampleName + "." + trackName : sampleName;
    }

    /**
     * Trim the alleles in inputVC from the reverse direction
     *
     * @param inputVC a non-null input VC whose alleles might need a haircut
     * @return a non-null VariantContext (may be == to inputVC) with alleles trimmed up
     */
    public static VariantContext reverseTrimAlleles( final VariantContext inputVC ) {
        return trimAlleles(inputVC, false, true);
    }

    /**
     * Trim the alleles in inputVC from the forward direction
     *
     * @param inputVC a non-null input VC whose alleles might need a haircut
     * @return a non-null VariantContext (may be == to inputVC) with alleles trimmed up
     */
    public static VariantContext forwardTrimAlleles( final VariantContext inputVC ) {
        return trimAlleles(inputVC, true, false);
    }

    /**
     * Trim the alleles in inputVC forward and reverse, as requested
     *
     * @param inputVC a non-null input VC whose alleles might need a haircut
     * @param trimForward should we trim up the alleles from the forward direction?
     * @param trimReverse should we trim up the alleles from the reverse direction?
     * @return a non-null VariantContext (may be == to inputVC) with trimmed up alleles
     */
    @Ensures("result != null")
    public static VariantContext trimAlleles(final VariantContext inputVC, final boolean trimForward, final boolean trimReverse) {
        if ( inputVC == null ) throw new IllegalArgumentException("inputVC cannot be null");

        if ( inputVC.getNAlleles() <= 1 || inputVC.isSNP() )
            return inputVC;

        // see whether we need to trim common reference base from all alleles
        final int revTrim = trimReverse ? computeReverseClipping(inputVC.getAlleles(), inputVC.getReference().getDisplayString().getBytes()) : 0;
        final VariantContext revTrimVC = trimAlleles(inputVC, -1, revTrim);
        final int fwdTrim = trimForward ? computeForwardClipping(revTrimVC.getAlleles()) : -1;
        final VariantContext vc= trimAlleles(revTrimVC, fwdTrim, 0);
        return vc;
    }

    /**
     * Trim up alleles in inputVC, cutting out all bases up to fwdTrimEnd inclusive and
     * the last revTrim bases from the end
     *
     * @param inputVC a non-null input VC
     * @param fwdTrimEnd bases up to this index (can be -1) will be removed from the start of all alleles
     * @param revTrim the last revTrim bases of each allele will be clipped off as well
     * @return a non-null VariantContext (may be == to inputVC) with trimmed up alleles
     */
    @Requires({"inputVC != null"})
    @Ensures("result != null")
    protected static VariantContext trimAlleles(final VariantContext inputVC,
                                                final int fwdTrimEnd,
                                                final int revTrim) {
        if( fwdTrimEnd == -1 && revTrim == 0 ) // nothing to do, so just return inputVC unmodified
            return inputVC;

        final List<Allele> alleles = new LinkedList<>();
        final Map<Allele, Allele> originalToTrimmedAlleleMap = new HashMap<>();

        for (final Allele a : inputVC.getAlleles()) {
            if (a.isSymbolic()) {
                alleles.add(a);
                originalToTrimmedAlleleMap.put(a, a);
            } else {
                // get bases for current allele and create a new one with trimmed bases
                final byte[] newBases = Arrays.copyOfRange(a.getBases(), fwdTrimEnd+1, a.length()-revTrim);
                final Allele trimmedAllele = Allele.create(newBases, a.isReference());
                alleles.add(trimmedAllele);
                originalToTrimmedAlleleMap.put(a, trimmedAllele);
            }
        }

        // now we can recreate new genotypes with trimmed alleles
        final AlleleMapper alleleMapper = new AlleleMapper(originalToTrimmedAlleleMap);
        final GenotypesContext genotypes = updateGenotypesWithMappedAlleles(inputVC.getGenotypes(), alleleMapper);

        final int start = inputVC.getStart() + (fwdTrimEnd + 1);
        final VariantContextBuilder builder = new VariantContextBuilder(inputVC);
        builder.start(start);
        builder.stop(start + alleles.get(0).length() - 1);
        builder.alleles(alleles);
        builder.genotypes(genotypes);
        return builder.make();
    }

    @Requires("originalGenotypes != null && alleleMapper != null")
    protected static GenotypesContext updateGenotypesWithMappedAlleles(final GenotypesContext originalGenotypes, final AlleleMapper alleleMapper) {
        final GenotypesContext updatedGenotypes = GenotypesContext.create(originalGenotypes.size());

        for ( final Genotype genotype : originalGenotypes ) {
            final List<Allele> updatedAlleles = alleleMapper.remap(genotype.getAlleles());
            updatedGenotypes.add(new GenotypeBuilder(genotype).alleles(updatedAlleles).make());
        }

        return updatedGenotypes;
    }

    public static int computeReverseClipping(final List<Allele> unclippedAlleles, final byte[] ref) {
        int clipping = 0;
        boolean stillClipping = true;

        while ( stillClipping ) {
            for ( final Allele a : unclippedAlleles ) {
                if ( a.isSymbolic() )
                    continue;

                // we need to ensure that we don't reverse clip out all of the bases from an allele because we then will have the wrong
                // position set for the VariantContext (although it's okay to forward clip it all out, because the position will be fine).
                if ( a.length() - clipping == 0 )
                    return clipping - 1;

                if ( a.length() - clipping <= 0 || a.length() == 0 ) {
                    stillClipping = false;
                }
                else if ( ref.length == clipping ) {
                    return -1;
                }
                else if ( a.getBases()[a.length()-clipping-1] != ref[ref.length-clipping-1] ) {
                    stillClipping = false;
                }
            }
            if ( stillClipping )
                clipping++;
        }

        return clipping;
    }

    /**
     * Clip out any unnecessary bases off the front of the alleles
     *
     * The VCF spec represents alleles as block substitutions, replacing AC with A for a
     * 1 bp deletion of the C.  However, it's possible that we'd end up with alleles that
     * contain extra bases on the left, such as GAC/GA to represent the same 1 bp deletion.
     * This routine finds an offset among all alleles that can be safely trimmed
     * off the left of each allele and still represent the same block substitution.
     *
     * A/C => A/C
     * AC/A => AC/A
     * ACC/AC => CC/C
     * AGT/CAT => AGT/CAT
     * <DEL>/C => <DEL>/C
     *
     * @param unclippedAlleles a non-null list of alleles that we want to clip
     * @return the offset into the alleles where we can safely clip, inclusive, or
     *   -1 if no clipping is tolerated.  So, if the result is 0, then we can remove
     *   the first base of every allele.  If the result is 1, we can remove the
     *   second base.
     */
    public static int computeForwardClipping(final List<Allele> unclippedAlleles) {
        // cannot clip unless there's at least 1 alt allele
        if ( unclippedAlleles.size() <= 1 )
            return -1;

        // we cannot forward clip any set of alleles containing a symbolic allele
        int minAlleleLength = Integer.MAX_VALUE;
        for ( final Allele a : unclippedAlleles ) {
            if ( a.isSymbolic() )
                return -1;
            minAlleleLength = Math.min(minAlleleLength, a.length());
        }

        final byte[] firstAlleleBases = unclippedAlleles.get(0).getBases();
        int indexOflastSharedBase = -1;

        // the -1 to the stop is that we can never clip off the right most base
        for ( int i = 0; i < minAlleleLength - 1; i++) {
            final byte base = firstAlleleBases[i];

            for ( final Allele allele : unclippedAlleles ) {
                if ( allele.getBases()[i] != base )
                    return indexOflastSharedBase;
            }

            indexOflastSharedBase = i;
        }

        return indexOflastSharedBase;
    }

    public static double computeHardyWeinbergPvalue(VariantContext vc) {
        if ( vc.getCalledChrCount() == 0 )
            return 0.0;
        return HardyWeinbergCalculation.hwCalculate(vc.getHomRefCount(), vc.getHetCount(), vc.getHomVarCount());
    }

    public static boolean requiresPaddingBase(final List<String> alleles) {

        // see whether one of the alleles would be null if trimmed through

        for ( final String allele : alleles ) {
            if ( allele.isEmpty() )
                return true;
        }

        int clipping = 0;
        Character currentBase = null;

        while ( true ) {
            for ( final String allele : alleles ) {
                if ( allele.length() - clipping == 0 )
                    return true;

                char myBase = allele.charAt(clipping);
                if ( currentBase == null )
                    currentBase = myBase;
                else if ( currentBase != myBase )
                    return false;
            }

            clipping++;
            currentBase = null;
        }
    }

    private final static Map<String, Object> subsetAttributes(final CommonInfo igc, final Collection<String> keysToPreserve) {
        Map<String, Object> attributes = new HashMap<>(keysToPreserve.size());
        for ( final String key : keysToPreserve  ) {
            if ( igc.hasAttribute(key) )
                attributes.put(key, igc.getAttribute(key));
        }
        return attributes;
    }

    /**
     * @deprecated use variant context builder version instead
     * @param vc                  the variant context
     * @param keysToPreserve      the keys to preserve
     * @return a pruned version of the original variant context
     */
    @Deprecated
    public static VariantContext pruneVariantContext(final VariantContext vc, Collection<String> keysToPreserve ) {
        return pruneVariantContext(new VariantContextBuilder(vc), keysToPreserve).make();
    }

    public static VariantContextBuilder pruneVariantContext(final VariantContextBuilder builder, Collection<String> keysToPreserve ) {
        final VariantContext vc = builder.make();
        if ( keysToPreserve == null ) keysToPreserve = Collections.emptyList();

        // VC info
        final Map<String, Object> attributes = subsetAttributes(vc.getCommonInfo(), keysToPreserve);

        // Genotypes
        final GenotypesContext genotypes = GenotypesContext.create(vc.getNSamples());
        for ( final Genotype g : vc.getGenotypes() ) {
            final GenotypeBuilder gb = new GenotypeBuilder(g);
            // remove AD, DP, PL, and all extended attributes, keeping just GT and GQ
            gb.noAD().noDP().noPL().noAttributes();
            genotypes.add(gb.make());
        }

        return builder.genotypes(genotypes).attributes(attributes);
    }

    public static boolean allelesAreSubset(VariantContext vc1, VariantContext vc2) {
        // if all alleles of vc1 are a contained in alleles of vc2, return true
        if (!vc1.getReference().equals(vc2.getReference()))
            return false;

        for (final Allele a :vc1.getAlternateAlleles()) {
            if (!vc2.getAlternateAlleles().contains(a))
                return false;
        }

        return true;
    }

    public static Map<VariantContext.Type, List<VariantContext>> separateVariantContextsByType( final Collection<VariantContext> VCs ) {
        if( VCs == null ) { throw new IllegalArgumentException("VCs cannot be null."); }

        final HashMap<VariantContext.Type, List<VariantContext>> mappedVCs = new HashMap<>();
        for ( final VariantContext vc : VCs ) {
            VariantContext.Type vcType = vc.getType();
            if( vc.hasAllele(NON_REF_SYMBOLIC_ALLELE) ) {
                if( vc.getAlternateAlleles().size() > 1 ) { throw new IllegalStateException("Reference records should not have more than one alternate allele"); }
                vcType = VariantContext.Type.NO_VARIATION;
            }

            // look at previous variant contexts of different type. If:
            // a) otherVC has alleles which are subset of vc, remove otherVC from its list and add otherVC to vc's list
            // b) vc has alleles which are subset of otherVC. Then, add vc to otherVC's type list (rather, do nothing since vc will be added automatically to its list)
            // c) neither: do nothing, just add vc to its own list
            boolean addtoOwnList = true;
            for (final VariantContext.Type type : VariantContext.Type.values()) {
                if (type.equals(vcType))
                    continue;

                if (!mappedVCs.containsKey(type))
                    continue;

                List<VariantContext> vcList = mappedVCs.get(type);
                for (int k=0; k <  vcList.size(); k++) {
                    VariantContext otherVC = vcList.get(k);
                    if (allelesAreSubset(otherVC,vc)) {
                        // otherVC has a type different than vc and its alleles are a subset of vc: remove otherVC from its list and add it to vc's type list
                        vcList.remove(k);
                        // avoid having empty lists
                        if (vcList.size() == 0)
                            mappedVCs.remove(type);
                        if ( !mappedVCs.containsKey(vcType) )
                            mappedVCs.put(vcType, new ArrayList<VariantContext>());
                        mappedVCs.get(vcType).add(otherVC);
                        break;
                    }
                    else if (allelesAreSubset(vc,otherVC)) {
                        // vc has a type different than otherVC and its alleles are a subset of VC: add vc to otherVC's type list and don't add to its own
                        mappedVCs.get(type).add(vc);
                        addtoOwnList = false;
                        break;
                    }
                }
            }
            if (addtoOwnList) {
                if ( !mappedVCs.containsKey(vcType) )
                    mappedVCs.put(vcType, new ArrayList<VariantContext>());
                mappedVCs.get(vcType).add(vc);
            }
        }

        return mappedVCs;
    }

    public static VariantContext purgeUnallowedGenotypeAttributes(VariantContext vc, Set<String> allowedAttributes) {
        if ( allowedAttributes == null )
            return vc;

        final GenotypesContext newGenotypes = GenotypesContext.create(vc.getNSamples());
        for ( final Genotype genotype : vc.getGenotypes() ) {
            final Map<String, Object> attrs = new HashMap<>();
            for ( final Map.Entry<String, Object> attr : genotype.getExtendedAttributes().entrySet() ) {
                if ( allowedAttributes.contains(attr.getKey()) )
                    attrs.put(attr.getKey(), attr.getValue());
            }
            newGenotypes.add(new GenotypeBuilder(genotype).attributes(attrs).make());
        }

        return new VariantContextBuilder(vc).genotypes(newGenotypes).make();
    }


    protected static class AlleleMapper {
        private VariantContext vc = null;
        private Map<Allele, Allele> map = null;
        public AlleleMapper(VariantContext vc)          { this.vc = vc; }
        public AlleleMapper(Map<Allele, Allele> map)    { this.map = map; }
        public boolean needsRemapping()                 { return this.map != null; }
        public Collection<Allele> values()              { return map != null ? map.values() : vc.getAlleles(); }
        public Allele remap(Allele a)                   { return map != null && map.containsKey(a) ? map.get(a) : a; }

        public List<Allele> remap(List<Allele> as) {
            List<Allele> newAs = new ArrayList<>();
            for ( final Allele a : as ) {
                //System.out.printf("  Remapping %s => %s%n", a, remap(a));
                newAs.add(remap(a));
            }
            return newAs;
        }

        /**
         * @return the list of unique values
         */
        public List<Allele> getUniqueMappedAlleles() {
            if ( map == null )
                return Collections.emptyList();
            return new ArrayList<>(new HashSet<>(map.values()));
        }
    }

    private static class CompareByPriority implements Comparator<VariantContext>, Serializable {
        List<String> priorityListOfVCs;
        public CompareByPriority(List<String> priorityListOfVCs) {
            this.priorityListOfVCs = priorityListOfVCs;
        }

        private int getIndex(VariantContext vc) {
            int i = priorityListOfVCs.indexOf(vc.getSource());
            if ( i == -1 ) throw new IllegalArgumentException("Priority list " + priorityListOfVCs + " doesn't contain variant context " + vc.getSource());
            return i;
        }

        public int compare(VariantContext vc1, VariantContext vc2) {
            return Integer.valueOf(getIndex(vc1)).compareTo(getIndex(vc2));
        }
    }

    /**
     * For testing purposes only.  Create a site-only VariantContext at contig:start containing alleles
     *
     * @param name the name of the VC
     * @param contig the contig for the VC
     * @param start the start of the VC
     * @param alleleStrings a non-null, non-empty list of strings for the alleles.  The first will be the ref allele, and others the
     *                      alt.  Will compute the stop of the VC from the length of the reference allele
     * @return a non-null VariantContext
     */
    public static VariantContext makeFromAlleles(final String name, final String contig, final int start, final List<String> alleleStrings) {
        if ( alleleStrings == null || alleleStrings.isEmpty() )
            throw new IllegalArgumentException("alleleStrings must be non-empty, non-null list");

        final List<Allele> alleles = new LinkedList<>();
        final int length = alleleStrings.get(0).length();

        boolean first = true;
        for ( final String alleleString : alleleStrings ) {
            alleles.add(Allele.create(alleleString, first));
            first = false;
        }
      return new VariantContextBuilder(name, contig, start, start+length-1, alleles).make();
    }

    /**
     * Splits the alleles for the provided variant context into its primitive parts.
     * Requires that the input VC be bi-allelic, so calling methods should first call splitVariantContextToBiallelics() if needed.
     * Currently works only for MNPs.
     *
     * @param vc  the non-null VC to split
     * @return a non-empty list of VCs split into primitive parts or the original VC otherwise
     */
    public static List<VariantContext> splitIntoPrimitiveAlleles(final VariantContext vc) {
        if ( vc == null )
            throw new IllegalArgumentException("Trying to break a null Variant Context into primitive parts");

        if ( !vc.isBiallelic() )
            throw new IllegalArgumentException("Trying to break a multi-allelic Variant Context into primitive parts");

        // currently only works for MNPs
        if ( !vc.isMNP() )
            return Arrays.asList(vc);

        final byte[] ref = vc.getReference().getBases();
        final byte[] alt = vc.getAlternateAllele(0).getBases();

        if ( ref.length != alt.length )
            throw new IllegalStateException("ref and alt alleles for MNP have different lengths");

        final List<VariantContext> result = new ArrayList<>(ref.length);

        for ( int i = 0; i < ref.length; i++ ) {

            // if the ref and alt bases are different at a given position, create a new SNP record (otherwise do nothing)
            if ( ref[i] != alt[i] ) {

                // create the ref and alt SNP alleles
                final Allele newRefAllele = Allele.create(ref[i], true);
                final Allele newAltAllele = Allele.create(alt[i], false);

                // create a new VariantContext with the new SNP alleles
                final VariantContextBuilder newVC = new VariantContextBuilder(vc).start(vc.getStart() + i).stop(vc.getStart() + i).alleles(Arrays.asList(newRefAllele, newAltAllele));

                // create new genotypes with updated alleles
                final Map<Allele, Allele> alleleMap = new HashMap<>();
                alleleMap.put(vc.getReference(), newRefAllele);
                alleleMap.put(vc.getAlternateAllele(0), newAltAllele);
                final GenotypesContext newGenotypes = updateGenotypesWithMappedAlleles(vc.getGenotypes(), new AlleleMapper(alleleMap));

                result.add(newVC.genotypes(newGenotypes).make());
            }
        }

        if ( result.isEmpty() )
            result.add(vc);

        return result;
    }

    /**
     * Are vc1 and 2 equal including their position and alleles?
     * @param vc1 non-null VariantContext
     * @param vc2 non-null VariantContext
     * @return true if vc1 and vc2 are equal, false otherwise
     */
    public static boolean equalSites(final VariantContext vc1, final VariantContext vc2) {
        if ( vc1 == null ) throw new IllegalArgumentException("vc1 cannot be null");
        if ( vc2 == null ) throw new IllegalArgumentException("vc2 cannot be null");

        if ( vc1.getStart() != vc2.getStart() ) return false;
        if ( vc1.getEnd() != vc2.getEnd() ) return false;
        if ( ! vc1.getChr().equals(vc2.getChr())) return false;
        if ( ! vc1.getAlleles().equals(vc2.getAlleles()) ) return false;
        return true;
    }

    /**
     * Returns the absolute 0-based index of an allele.
     *
     * <p/>
     * If the allele is equal to the reference, the result is 0, if it equal to the first alternative the result is 1
     * and so forth.
     * <p/>
     * Therefore if you want the 0-based index within the alternative alleles you need to do the following:
     *
     * <p/>
     * You can indicate whether the Java object reference comparator {@code ==} can be safelly used by setting {@code useEquals} to {@code false}.
     *
     * @param vc the target variant context.
     * @param allele the target allele.
     * @param ignoreRefState whether the reference states of the allele is important at all. Has no effect if {@code useEquals} is {@code false}.
     * @param considerRefAllele whether the reference allele should be considered. You should set it to {@code false} if you are only interested in alternative alleles.
     * @param useEquals whether equal method should be used in the search: {@link Allele#equals(Allele,boolean)}.
     *
     * @throws IllegalArgumentException if {@code allele} is {@code null}.
     * @return {@code -1} if there is no such allele that satify those criteria, a value between 0 and {@link VariantContext#getNAlleles()} {@code -1} otherwise.
     */
    public static int indexOfAllele(final VariantContext vc, final Allele allele, final boolean ignoreRefState, final boolean considerRefAllele, final boolean useEquals) {
        if (allele == null) throw new IllegalArgumentException();
        return useEquals ? indexOfEqualAllele(vc,allele,ignoreRefState,considerRefAllele) : indexOfSameAllele(vc,allele,considerRefAllele);
    }

    /**
     * Returns the relative 0-based index of an alternative allele.
     * <p/>
     * The the query allele is the same as the first alternative allele, the result is 0,
     * if it is equal to the second 1 and so forth.
     *
     *
     * <p/>
     * Notice that the ref-status of the query {@code allele} is ignored.
     *
     * @param vc the target variant context.
     * @param allele the query allele.
     * @param useEquals  whether equal method should be used in the search: {@link Allele#equals(Allele,boolean)}.
     *
     * @throws IllegalArgumentException if {@code allele} is {@code null}.
     *
     * @return {@code -1} if there is no such allele that satify those criteria, a value between 0 and the number
     *  of alternative alleles - 1.
     */
    public static int indexOfAltAllele(final VariantContext vc, final Allele allele, final boolean useEquals) {
        final int absoluteIndex = indexOfAllele(vc,allele,true,false,useEquals);
        return absoluteIndex == -1 ? -1 : absoluteIndex - 1;
    }

    // Impements index search using equals.
    private static int indexOfEqualAllele(final VariantContext vc, final Allele allele, final boolean ignoreRefState,
                                          final boolean considerRefAllele) {
        int i = 0;
        for (final Allele a : vc.getAlleles())
            if (a.equals(allele,ignoreRefState))
                return i == 0 ? (considerRefAllele ? 0 : -1) : i;
            else
                i++;
        return -1;
    }

    // Implements index search using ==.
    private static int indexOfSameAllele(final VariantContext vc, final Allele allele, final boolean considerRefAllele) {
        int i = 0;

        for (final Allele a : vc.getAlleles())
            if (a == allele)
                return i == 0 ? (considerRefAllele ? 0 : -1) : i;
            else
                i++;

        return -1;
    }
}
