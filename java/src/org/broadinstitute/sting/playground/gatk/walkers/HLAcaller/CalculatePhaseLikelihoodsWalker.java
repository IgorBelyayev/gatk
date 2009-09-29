/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.broadinstitute.sting.playground.gatk.walkers.HLAcaller;
import net.sf.samtools.SAMRecord;
import org.broadinstitute.sting.gatk.walkers.*;
import org.broadinstitute.sting.utils.cmdLine.Argument;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author shermanjia
 */
@Requires({DataSource.READS, DataSource.REFERENCE})
public class CalculatePhaseLikelihoodsWalker extends ReadWalker<Integer, Integer> {
    @Argument(fullName = "baseLikelihoods", shortName = "bl", doc = "Base likelihoods file", required = true)
    public String baseLikelihoodsFile = "";

    @Argument(fullName = "debugHLA", shortName = "debugHLA", doc = "Print debug", required = false)
    public boolean DEBUG = false;

    @Argument(fullName = "filter", shortName = "filter", doc = "file containing reads to exclude", required = false)
    public String filterFile = "";

    @Argument(fullName = "ethnicity", shortName = "ethnicity", doc = "Use allele frequencies for this ethnic group", required = false)
    public String ethnicity = "Caucasian";

    @Argument(fullName = "debugAlleles", shortName = "debugAlleles", doc = "Print likelihood scores for these alleles", required = false)
    public String debugAlleles = "";

    @Argument(fullName = "onlyfrequent", shortName = "onlyfrequent", doc = "Only consider alleles with frequency > 0.0001", required = false)
    public boolean ONLYFREQUENT = false;

    String CaucasianAlleleFrequencyFile = "/humgen/gsa-scr1/GSA/sjia/454_HLA/HLA/HLA_CaucasiansUSA.freq";
    String BlackAlleleFrequencyFile = "/humgen/gsa-scr1/GSA/sjia/454_HLA/HLA/HLA_BlackUSA.freq";
    String AlleleFrequencyFile;
    
    String HLAdatabaseFile ="/humgen/gsa-scr1/GSA/sjia/454_HLA/HLA/HLA.nuc.imputed.4digit.sam";
    SAMFileReader HLADictionaryReader = new SAMFileReader();
    boolean HLAdataLoaded = false;
    String[] HLAnames, HLAreads;
    ArrayList<String> ReadsToDiscard;
    Integer[] HLAstartpos, HLAstoppos, PolymorphicSites;
    
    int[][] numObservations, totalObservations;
    int[] SNPnumInRead, SNPposInRead;
    CigarParser cigarparser = new CigarParser();
    Hashtable AlleleFrequencies;

    public Integer reduceInit() {

        if (!HLAdataLoaded){
            HLAdataLoaded = true;
            //Load HLA dictionary
            out.printf("INFO  Loading HLA dictionary ... ");
            
            HLADictionaryReader.ReadFile(HLAdatabaseFile);
            HLAreads = HLADictionaryReader.GetReads();
            HLAnames = HLADictionaryReader.GetReadNames();
            HLAstartpos = HLADictionaryReader.GetStartPositions();
            HLAstoppos = HLADictionaryReader.GetStopPositions();
            out.printf("Done! %s HLA alleles loaded.\n",HLAreads.length);

            if (!filterFile.equals("")){
                out.printf("INFO  Reading properties file ... ");
                SimilarityFileReader similarityReader = new SimilarityFileReader();
                similarityReader.ReadFile(filterFile);
                ReadsToDiscard = similarityReader.GetReadsToDiscard();
                out.printf("Done! Found %s misaligned reads to discard.\n",ReadsToDiscard.size());
            }


            //Reading base likelihoods
            if (!baseLikelihoodsFile.equals("")){
                //Get only sites found to be different from reference
                out.printf("INFO  Reading base likelihoods file ... ");
                BaseLikelihoodsFileReader baseLikelihoodsReader = new BaseLikelihoodsFileReader();
                baseLikelihoodsReader.ReadFile(baseLikelihoodsFile, true);
                PolymorphicSites = baseLikelihoodsReader.GetPolymorphicSites();
                out.printf("%s polymorphic sites found\n",PolymorphicSites.length);
            }else{
                //use all sites in class 1 HLA
                out.printf("INFO  Using all positions in the classical HLA genes ... ");
                PolymorphicSites = InitializePolymorphicSites();
            }
            int l = PolymorphicSites.length;
            SNPnumInRead = new int[l];
            SNPposInRead = new int[l];
            numObservations = new int[l*5][l*5];
            totalObservations = new int[l][l];

            //Read allele frequencies
            if (ethnicity.equals("Black")){
                AlleleFrequencyFile = BlackAlleleFrequencyFile;
            }else{
                AlleleFrequencyFile = CaucasianAlleleFrequencyFile;
            }
            out.printf("INFO  Reading HLA allele frequencies ... ");
            FrequencyFileReader HLAfreqReader = new FrequencyFileReader();
            HLAfreqReader.ReadFile(AlleleFrequencyFile);
            AlleleFrequencies = HLAfreqReader.GetAlleleFrequencies();
            out.printf("Done! Frequencies for %s HLA alleles loaded.\n",AlleleFrequencies.size());

            /*
            PolymorphicSites = FindPolymorphicSites(HLADictionaryReader.GetMinStartPos(),HLADictionaryReader.GetMaxStopPos());
            */

        }
        return 0;
    }


    public Integer map(char[] ref, SAMRecord read) {
        if (!ReadsToDiscard.contains(read.getReadName())){
            UpdateCorrelation(read);
        }else{
            
        }
        return 1;
    }

    public Integer reduce(Integer value, Integer sum) {
        return value + sum;
    }

    public void onTraversalDone(Integer numreads) {
        String name1, name2;
        Double frq1, frq2, likelihood, minfrq = 0.0;
        int numCombinations = 0;


        if (!debugAlleles.equals("")){
            String s[] = debugAlleles.split(",");
            int index1 = HLADictionaryReader.GetReadIndex(s[0]);
            int index2 = HLADictionaryReader.GetReadIndex(s[1]);
            if (index1 > -1 && index2 > -1){
                likelihood = CalculatePhaseLikelihood(index1,index2,true);
            }
        }

        if (ONLYFREQUENT){
            minfrq = 0.0001;
        }

        out.printf("NUM\tAllele1\tAllele2\tPhase\tFrq1\tFrq2\n");
        for (int i = 0; i < HLAnames.length; i++){
            name1 = HLAnames[i].substring(4);
            if (AlleleFrequencies.containsKey(name1)){
                frq1 = Double.parseDouble((String) AlleleFrequencies.get(name1).toString());
                if (frq1 > minfrq){
                    for (int j = i; j < HLAnames.length; j++){
                        name2 = HLAnames[j].substring(4);
                        if (name1.substring(0,1).equals(name2.substring(0,1))){
                            if (AlleleFrequencies.containsKey(name2)){
                                frq2 = Double.parseDouble((String) AlleleFrequencies.get(name2).toString());
                                if (frq2 > minfrq){
                                    likelihood = CalculatePhaseLikelihood(i,j,false);
                                    numCombinations++;
                                    out.printf("%s\t%s\t%s\t%.2f\t%.2f\t%.2f\n",numCombinations,name1,name2,likelihood,Math.log10(frq1),Math.log10(frq2));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private Integer[] InitializePolymorphicSites(){
        int HLA_A_start = 30018310, HLA_A_end = 30021211, num_A_positions = HLA_A_end - HLA_A_start + 1;
        int HLA_B_start = 31430239, HLA_B_end = 31432914, num_B_positions = HLA_B_end - HLA_B_start + 1;
        int HLA_C_start = 31344925, HLA_C_end = 31347827, num_C_positions = HLA_C_end - HLA_C_start + 1;
        Integer[] polymorphicSites = new Integer[num_A_positions+num_B_positions+num_C_positions];
        for (int i = 0; i < num_A_positions; i++){
            polymorphicSites[i]=HLA_A_start + i;
        }
        for (int i = 0; i < num_C_positions; i++){
            polymorphicSites[i+num_A_positions]=HLA_C_start + i;
        }
        for (int i = 0; i < num_B_positions; i++){
            polymorphicSites[i+num_A_positions+num_C_positions]=HLA_B_start + i;
        }
        return polymorphicSites;
    }

    private int IndexOf(char c){
        switch(c){
            case 'A': return 0;
            case 'C': return 1;
            case 'G': return 2;
            case 'T': return 3;
            //case 'D': return 4;
            default: return -1;
        }
    }
    
    private void UpdateCorrelation(SAMRecord read){
        //Updates correlation table with SNPs from specific read (for phasing)
        String s = cigarparser.FormatRead(read.getCigarString(), read.getReadString());
        ArrayList<Integer> SNPsInRead = new ArrayList<Integer>();
        ArrayList<Integer> readindex = new ArrayList<Integer>();

        int readstart = read.getAlignmentStart();
        int readend = read.getAlignmentEnd();
        int numPositions = PolymorphicSites.length;
        char c1, c2;
        int a, b, i, j, SNPcount = 0;
        
        //Find all SNPs in read
        for (i = 0; i < numPositions; i++){
            if (PolymorphicSites[i] > readstart && PolymorphicSites[i] < readend){
                SNPnumInRead[i] = SNPcount;
                SNPposInRead[i] = PolymorphicSites[i]-readstart;
                SNPcount++;
            }else{
                SNPnumInRead[i] = -1;
                SNPposInRead[i] = -1;
            }
        }

        //Update correlation table; for each combination of SNP positions
        for (i = 0; i < numPositions; i++){
            if (SNPnumInRead[i] > -1){
                c1 = s.charAt(SNPposInRead[i]);
                if (IndexOf(c1) > -1){
                    for (j = i+1; j < numPositions; j ++){
                        if (SNPnumInRead[j] > -1){
                            c2 = s.charAt(SNPposInRead[j]);
                            if (IndexOf(c2) > -1){
                                a = i*5 + IndexOf(c1);
                                b = j*5 + IndexOf(c2);

                                numObservations[a][b]++;
                                totalObservations[i][j]++;
                                if (DEBUG){
                                    out.printf("DEBUG  %s\t%s %s\t[i=%s,j=%s]\t[%s,%s]\t[%s,%s]\n",read.getReadName(),PolymorphicSites[i],PolymorphicSites[j],i,j,c1,c2,a,b);
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    private double CalculatePhaseLikelihood(int alleleIndex1, int alleleIndex2, boolean PRINTDEBUG){
        //calculate the likelihood that the particular combination of alleles satisfies the phase count data
        double likelihood = 0, prob = 0;
        int readstart1 = HLAstartpos[alleleIndex1]; int readend1 = HLAstoppos[alleleIndex1];
        int readstart2 = HLAstartpos[alleleIndex2]; int readend2 = HLAstoppos[alleleIndex2];
        int combinedstart = Math.max(readstart1,readstart2);
        int combinedstop  = Math.min(readend1,readend2);
        
        int numPositions = PolymorphicSites.length, SNPcount = 0;
        int i, j, a1, a2, b1, b2;
        char c11, c12, c21, c22;
        int numInPhase = 0, numOutOfPhase;


        //Find all SNPs in read
        for (i = 0; i < numPositions; i++){
            if (PolymorphicSites[i] > combinedstart && PolymorphicSites[i] < combinedstop){
                SNPnumInRead[i] = SNPcount;
                SNPposInRead[i] = PolymorphicSites[i]-combinedstart;
                SNPcount++;
            }else{
                SNPnumInRead[i] = -1;
                SNPposInRead[i] = -1;
            }
        }

        String s1 = HLAreads[alleleIndex1];
        String s2 = HLAreads[alleleIndex2];
        //Iterate through every pairwise combination of SNPs, and update likelihood for the allele combination
        for (i = 0; i < numPositions; i++){
            if (SNPnumInRead[i] > -1){
                c11 = s1.charAt(SNPposInRead[i]);
                c21 = s2.charAt(SNPposInRead[i]);
                if (IndexOf(c11) > -1 && IndexOf(c21) > -1){
                    for (j = i+1; j < numPositions; j ++){
                        if (SNPnumInRead[j] > -1 && totalObservations[i][j] > 0){
                            c12 = s1.charAt(SNPposInRead[j]);
                            c22 = s2.charAt(SNPposInRead[j]);
                            if (IndexOf(c12) > -1 && IndexOf(c22) > -1){
                                a1 = i*5 + IndexOf(c11);
                                b1 = j*5 + IndexOf(c12);
                                a2 = i*5 + IndexOf(c21);
                                b2 = j*5 + IndexOf(c22);
                                //check if the two alleles are identical at the chosen 2 locations
                                if ((c11 == c21) && (c12 == c22)){
                                    numInPhase = numObservations[a1][b1];
                                }else{
                                    numInPhase = numObservations[a1][b1] + numObservations[a2][b2];
                                }
                                prob = Math.max((double) numInPhase / (double) totalObservations[i][j], 0.0001);
                                likelihood += Math.log10(prob);
                            
                                if (PRINTDEBUG){
                                    out.printf("DEBUG  %s %s %s[%s%s] %s[%s%s]\t[%s,%s]\t[%s,%s] [%s,%s]\t%s / %s\t%.4f\t%.2f\n",HLAnames[alleleIndex1],HLAnames[alleleIndex2],PolymorphicSites[i],c11,c21,PolymorphicSites[j],c12,c22, i,j,a1,b1,a2,b2,numInPhase,totalObservations[i][j],prob,likelihood);
                                }
                            }
                        }
                    }
                }
            }
        }
        return likelihood;
    }
}
