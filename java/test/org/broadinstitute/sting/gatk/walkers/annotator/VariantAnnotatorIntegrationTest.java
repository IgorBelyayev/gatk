package org.broadinstitute.sting.gatk.walkers.annotator;

import org.broadinstitute.sting.WalkerTest;
import org.broadinstitute.sting.utils.StingException;
import org.junit.Test;

import java.util.Arrays;

public class VariantAnnotatorIntegrationTest extends WalkerTest {

    public static String secondBaseTestString() {
        return "-T VariantAnnotator -R /seq/references/Homo_sapiens_assembly18/v0/Homo_sapiens_assembly18.fasta -vcf %s -A SecondBaseSkew";
    }

    public static String validationDataPath() {
         return "/humgen/gsa-scr1/GATK_Data/Validation_Data/";
    }

    public static String secondBaseTestFile( int testNo ) {
        switch ( testNo ) {
            case 1: return "NA12891";
            case 2: return "NA20762";
            default: throw new StingException("Impossible test has been run: secondbasetest number "+ testNo);
        }
    }

    public static String secondBaseTestInterval ( int testNo ) {
        switch ( testNo ) {
            case 1: return "-L chr1:14,000,000-18,000,000";
            case 2: return "-L chr22:20660081-20660083 -L chr22:29198100-29198104 -L chr22:29821330-29821334";
            default: throw new StingException("Impossible test has been run: secondbasetest number "+testNo);
        }
    }

    public static String secondBaseTestmd5( int testNo ) {
        switch ( testNo ) {
            case 1: return "48adec9c7fa6d3f5f5647b8eababd4e3";
            case 2: return "3a53d945b38e1fc87a801f23115222fe";
            default: throw new StingException("Impossible test has been run: secondbasetest number "+testNo);
        }
    }
    
    public static String baseTestString() {
        return "-T VariantAnnotator -R /broad/1KG/reference/human_b36_both.fasta -vcf %s";
    }



    @Test
    public void testHasAnnotsNotAsking1() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseTestString() + " -B variant,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample2.vcf -I /humgen/gsa-scr1/GATK_Data/Validation_Data/low_coverage_CEU.chr1.10k-11k.bam -L 1:10,020,000-10,021,000", 1,
                Arrays.asList("0ff01251afdabafb4e137357b25be72a"));
        executeTest("test file has annotations, not asking for annotations, #1", spec);
    }

    @Test
    public void testHasAnnotsNotAsking2() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseTestString() + " -B variant,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample3.vcf -I /humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,000-10,050,000", 1,
                Arrays.asList("227f38b2f322780187eacd0c98ace3e6"));
        executeTest("test file has annotations, not asking for annotations, #2", spec);
    }

    @Test
    public void testHasAnnotsAsking1() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseTestString() + " -all -B variant,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample2.vcf -I /humgen/gsa-scr1/GATK_Data/Validation_Data/low_coverage_CEU.chr1.10k-11k.bam -L 1:10,020,000-10,021,000", 1,
                Arrays.asList("3b54731a929134410f167630ec434310"));
        executeTest("test file has annotations, asking for annotations, #1", spec);
    }

    @Test
    public void testHasAnnotsAsking2() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseTestString() + " -all -B variant,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample3.vcf -I /humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,000-10,050,000", 1,
                Arrays.asList("c713d6d6cf922eab4150737f51f13abc"));
        executeTest("test file has annotations, asking for annotations, #2", spec);
    }

    @Test
    public void testNoAnnotsNotAsking1() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseTestString() + " -B variant,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample2empty.vcf -I /humgen/gsa-scr1/GATK_Data/Validation_Data/low_coverage_CEU.chr1.10k-11k.bam -L 1:10,020,000-10,021,000", 1,
                Arrays.asList("18d85a9711d56bdf7e2327b83d6745e2"));
        executeTest("test file doesn't have annotations, not asking for annotations, #1", spec);
    }

    @Test
    public void testNoAnnotsNotAsking2() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseTestString() + " -B variant,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample3empty.vcf -I /humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,000-10,050,000", 1,
                Arrays.asList("da51f18d189fb0fd804822c81b786e0f"));
        executeTest("test file doesn't have annotations, not asking for annotations, #2", spec);
    }

    @Test
    public void testNoAnnotsAsking1() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseTestString() + " -all -B variant,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample2empty.vcf -I /humgen/gsa-scr1/GATK_Data/Validation_Data/low_coverage_CEU.chr1.10k-11k.bam -L 1:10,020,000-10,021,000", 1,
                Arrays.asList("0feb1dd8aae945ce5f05c1156acadd3c"));
        executeTest("test file doesn't have annotations, asking for annotations, #1", spec);
    }

    @Test
    public void testNoAnnotsAsking2() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseTestString() + " -all -B variant,VCF,/humgen/gsa-scr1/GATK_Data/Validation_Data/vcfexample3empty.vcf -I /humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,000-10,050,000", 1,
                Arrays.asList("b15b965af6d204cbb2926f59f68d987c"));
        executeTest("test file doesn't have annotations, asking for annotations, #2", spec);
    }

}