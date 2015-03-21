/*
*  By downloading the PROGRAM you agree to the following terms of use:
*
*  BROAD INSTITUTE - SOFTWARE LICENSE AGREEMENT - FOR ACADEMIC NON-COMMERCIAL RESEARCH PURPOSES ONLY
*
*  This Agreement is made between the Broad Institute, Inc. with a principal address at 7 Cambridge Center, Cambridge, MA 02142 (BROAD) and the LICENSEE and is effective at the date the downloading is completed (EFFECTIVE DATE).
*
*  WHEREAS, LICENSEE desires to license the PROGRAM, as defined hereinafter, and BROAD wishes to have this PROGRAM utilized in the public interest, subject only to the royalty-free, nonexclusive, nontransferable license rights of the United States Government pursuant to 48 CFR 52.227-14; and
*  WHEREAS, LICENSEE desires to license the PROGRAM and BROAD desires to grant a license on the following terms and conditions.
*  NOW, THEREFORE, in consideration of the promises and covenants made herein, the parties hereto agree as follows:
*
*  1. DEFINITIONS
*  1.1 PROGRAM shall mean copyright in the object code and source code known as GATK2 and related documentation, if any, as they exist on the EFFECTIVE DATE and can be downloaded from http://www.broadinstitute/GATK on the EFFECTIVE DATE.
*
*  2. LICENSE
*  2.1   Grant. Subject to the terms of this Agreement, BROAD hereby grants to LICENSEE, solely for academic non-commercial research purposes, a non-exclusive, non-transferable license to: (a) download, execute and display the PROGRAM and (b) create bug fixes and modify the PROGRAM.
*  The LICENSEE may apply the PROGRAM in a pipeline to data owned by users other than the LICENSEE and provide these users the results of the PROGRAM provided LICENSEE does so for academic non-commercial purposes only.  For clarification purposes, academic sponsored research is not a commercial use under the terms of this Agreement.
*  2.2  No Sublicensing or Additional Rights. LICENSEE shall not sublicense or distribute the PROGRAM, in whole or in part, without prior written permission from BROAD.  LICENSEE shall ensure that all of its users agree to the terms of this Agreement.  LICENSEE further agrees that it shall not put the PROGRAM on a network, server, or other similar technology that may be accessed by anyone other than the LICENSEE and its employees and users who have agreed to the terms of this agreement.
*  2.3  License Limitations. Nothing in this Agreement shall be construed to confer any rights upon LICENSEE by implication, estoppel, or otherwise to any computer software, trademark, intellectual property, or patent rights of BROAD, or of any other entity, except as expressly granted herein. LICENSEE agrees that the PROGRAM, in whole or part, shall not be used for any commercial purpose, including without limitation, as the basis of a commercial software or hardware product or to provide services. LICENSEE further agrees that the PROGRAM shall not be copied or otherwise adapted in order to circumvent the need for obtaining a license for use of the PROGRAM.
*
*  3. OWNERSHIP OF INTELLECTUAL PROPERTY
*  LICENSEE acknowledges that title to the PROGRAM shall remain with BROAD. The PROGRAM is marked with the following BROAD copyright notice and notice of attribution to contributors. LICENSEE shall retain such notice on all copies.  LICENSEE agrees to include appropriate attribution if any results obtained from use of the PROGRAM are included in any publication.
*  Copyright 2012 Broad Institute, Inc.
*  Notice of attribution:  The GATK2 program was made available through the generosity of Medical and Population Genetics program at the Broad Institute, Inc.
*  LICENSEE shall not use any trademark or trade name of BROAD, or any variation, adaptation, or abbreviation, of such marks or trade names, or any names of officers, faculty, students, employees, or agents of BROAD except as states above for attribution purposes.
*
*  4. INDEMNIFICATION
*  LICENSEE shall indemnify, defend, and hold harmless BROAD, and their respective officers, faculty, students, employees, associated investigators and agents, and their respective successors, heirs and assigns, (Indemnitees), against any liability, damage, loss, or expense (including reasonable attorneys fees and expenses) incurred by or imposed upon any of the Indemnitees in connection with any claims, suits, actions, demands or judgments arising out of any theory of liability (including, without limitation, actions in the form of tort, warranty, or strict liability and regardless of whether such action has any factual basis) pursuant to any right or license granted under this Agreement.
*
*  5. NO REPRESENTATIONS OR WARRANTIES
*  THE PROGRAM IS DELIVERED AS IS.  BROAD MAKES NO REPRESENTATIONS OR WARRANTIES OF ANY KIND CONCERNING THE PROGRAM OR THE COPYRIGHT, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE. BROAD EXTENDS NO WARRANTIES OF ANY KIND AS TO PROGRAM CONFORMITY WITH WHATEVER USER MANUALS OR OTHER LITERATURE MAY BE ISSUED FROM TIME TO TIME.
*  IN NO EVENT SHALL BROAD OR ITS RESPECTIVE DIRECTORS, OFFICERS, EMPLOYEES, AFFILIATED INVESTIGATORS AND AFFILIATES BE LIABLE FOR INCIDENTAL OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING, WITHOUT LIMITATION, ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER BROAD SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
*
*  6. ASSIGNMENT
*  This Agreement is personal to LICENSEE and any rights or obligations assigned by LICENSEE without the prior written consent of BROAD shall be null and void.
*
*  7. MISCELLANEOUS
*  7.1 Export Control. LICENSEE gives assurance that it will comply with all United States export control laws and regulations controlling the export of the PROGRAM, including, without limitation, all Export Administration Regulations of the United States Department of Commerce. Among other things, these laws and regulations prohibit, or require a license for, the export of certain types of software to specified countries.
*  7.2 Termination. LICENSEE shall have the right to terminate this Agreement for any reason upon prior written notice to BROAD. If LICENSEE breaches any provision hereunder, and fails to cure such breach within thirty (30) days, BROAD may terminate this Agreement immediately. Upon termination, LICENSEE shall provide BROAD with written assurance that the original and all copies of the PROGRAM have been destroyed, except that, upon prior written authorization from BROAD, LICENSEE may retain a copy for archive purposes.
*  7.3 Survival. The following provisions shall survive the expiration or termination of this Agreement: Articles 1, 3, 4, 5 and Sections 2.2, 2.3, 7.3, and 7.4.
*  7.4 Notice. Any notices under this Agreement shall be in writing, shall specifically refer to this Agreement, and shall be sent by hand, recognized national overnight courier, confirmed facsimile transmission, confirmed electronic mail, or registered or certified mail, postage prepaid, return receipt requested.  All notices under this Agreement shall be deemed effective upon receipt.
*  7.5 Amendment and Waiver; Entire Agreement. This Agreement may be amended, supplemented, or otherwise modified only by means of a written instrument signed by all parties. Any waiver of any rights or failure to act in a specific instance shall relate only to such instance and shall not be construed as an agreement to waive any rights or fail to act in any other instance, whether or not similar. This Agreement constitutes the entire agreement among the parties with respect to its subject matter and supersedes prior agreements or understandings between the parties relating to its subject matter.
*  7.6 Binding Effect; Headings. This Agreement shall be binding upon and inure to the benefit of the parties and their respective permitted successors and assigns. All headings are for convenience only and shall not affect the meaning of any provision of this Agreement.
*  7.7 Governing Law. This Agreement shall be construed, governed, interpreted and applied in accordance with the internal laws of the Commonwealth of Massachusetts, U.S.A., without regard to conflict of laws principles.
*/

package org.broadinstitute.gatk.queue.qscripts.tecdev

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk._
import org.broadinstitute.gatk.queue.function.RetryMemoryLimit

class SingleSampleVaraintCallingScript extends QScript{
  qscript =>

  @Argument(shortName="out", doc="output file", required=true)
  var out: String = _
  @Argument(shortName="R", doc="ref file", required=true)
  var ref: String = _
  @Argument(shortName="I", doc="bam file", required=true)
  var bam: String = _
  @Argument(shortName="interval", doc="interval file", required=false)
  var intervalString: List[String] = List()
  @Argument(shortName="isr", doc="interval set rule", required=false)
  var intervalSetRule: org.broadinstitute.gatk.utils.interval.IntervalSetRule = org.broadinstitute.gatk.utils.interval.IntervalSetRule.UNION
  @Argument(shortName="intervalFile", doc="interval file", required=false)
  var intervalFiles: List[File] = List()
  @Argument(shortName="sc", doc="scatter count", required=false)
  var jobs: Int = 100
  @Argument(shortName="dr", doc="downsampling", required=false)
  var downsampling: Int = 250
  @Argument(shortName = "stand_call_conf", doc= "standard min confidence threshold for calling", required = false)
  var stand_call_conf: Double = 20
  @Argument(shortName = "stand_emit_conf", doc= "standard min confidence threshold for emitting", required = false)
  var stand_emit_conf: Double = 20
  @Argument(shortName = "dontUseSC", doc= "do not use the soft clipped bases in HC", required = false)
  var doNotUseSoftClippedBases = false
  @Argument(shortName = "headMerging", doc= "recover Dangling Heads (for RNAseq data)", required = false)
  var recoverDanglingHeads = false
  @Argument(shortName = "assess", doc="run AssessNA12878 on the VCF files", required = false)
  var assessNA12878 = false
  @Argument(shortName = "hc", doc="run HC", required = false)
  var useHC = true
  @Argument(shortName = "ug", doc="run UG", required = false)
  var useUG = false
  @Argument(shortName = "gvcf", doc= "run HC in gVCF mode", required = false)
  var useGVCF = false
  @Argument(shortName = "perBase", doc= "run HC in perBase gvcf output", required = false)
  var perBase = false


  //  trait UNIVERSAL_GATK_ARGS extends CommandLineGATK {
  //    memoryLimit = 2
  //    //this.unsafe = org.broadinstitute.sting.gatk.arguments.ValidationExclusion.TYPE.ALLOW_N_CIGAR_READS
  //
  //  }

  def script = {

    val hc = new hc_rnaMode(new File(qscript.out + ".hc.vcf"))
    val hc_filter = new filter(hc.out, swapExt(hc.out, ".vcf", ".hardFiltered.vcf"))

    if(useHC) {
      add(hc)
      add(hc_filter)
    }

    val ug = new ug_rnaMode(new File(qscript.out + ".ug.vcf"))
    val ug_filter = new filter(ug.out, swapExt(ug.out, ".vcf", ".hardFiltered.vcf"))

    if(useUG) {
      add(ug)
      add(ug_filter)
    }

    if(assessNA12878){
      val hc_assess = new assess(hc_filter.out)
      add(hc_assess)
      val ug_assess = new assess(ug_filter.out)
      add(ug_assess)
    }
  }

  case class assess (inVCF: File) extends AssessNA12878{
    this.memoryLimit = 2
    this.variant :+= inVCF
    this.reference_sequence = new File(qscript.ref)
    this.intervalsString :+= "20:10000000-24000000"
    this.intervals :+= new File("/seq/references/HybSelOligos/whole_exome_illumina_coding_v1/whole_exome_illumina_coding_v1.Homo_sapiens_assembly19.targets.interval_list")
    this.excludeIntervals :+= new File("/humgen/gsa-hpprojects/NA12878Collection/knowledgeBase/complexRegions.doNotAssess.interval_list")
    this.detailed = true
    this.allSites = true
    this.badSites = new File("allSites."+inVCF.getName())
    this.BAM = qscript.bam
    this.o = new File ("assess."+swapExt(inVCF,".vcf",".txt").getName())
  }

  case class filter (inVCF: File, outVCF: File) extends VariantFiltration{
    this.memoryLimit = 4
    this.reference_sequence = new File(qscript.ref)
    this.intervalsString = qscript.intervalString
    this.interval_set_rule = qscript.intervalSetRule
    this.intervals = qscript.intervalFiles
    this.variant = inVCF
    this.out = outVCF
    this.analysisName = "VarinatFiltration"
    this.filterName = Seq("FS","QD")
    this.filterExpression = Seq("FS > 30.0", "QD < 2.0")
    this.clusterWindowSize = 35
    this.clusterSize = 3
  }

  case class ug_rnaMode(outVCF: File) extends UnifiedGenotyper {
    this.memoryLimit = 2
    this.reference_sequence = new File(qscript.ref)
    this.intervalsString = qscript.intervalString
    this.interval_set_rule = qscript.intervalSetRule
    this.intervals = qscript.intervalFiles
    this.scatterCount = qscript.jobs
    this.input_file :+= new File(qscript.bam)
    this.out = outVCF
    this.glm = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypeLikelihoodsCalculationModel.Model.BOTH
    this.baq = org.broadinstitute.gatk.utils.baq.BAQ.CalculationMode.CALCULATE_AS_NECESSARY
    this.analysisName = "UnifiedGenotyper"
    this.stand_call_conf = qscript.stand_call_conf
    this.stand_emit_conf = qscript.stand_emit_conf
  }


  case class hc_rnaMode(outVCF: File) extends HaplotypeCaller with RetryMemoryLimit{
    this.memoryLimit = 4
    this.reference_sequence = new File(qscript.ref)
    this.intervalsString = qscript.intervalString
    this.interval_set_rule = qscript.intervalSetRule
    this.intervals = qscript.intervalFiles
    this.scatterCount = qscript.jobs
    this.input_file :+= new File(qscript.bam)
    this.out = outVCF
    this.downsample_to_coverage = qscript.downsampling
    if (qscript.doNotUseSoftClippedBases)
      this.dontUseSoftClippedBases = true
    if (qscript.recoverDanglingHeads)
      this.recoverDanglingHeads = true
    if (qscript.useGVCF){
      this.emitRefConfidence = org.broadinstitute.gatk.tools.walkers.haplotypecaller.ReferenceConfidenceMode.GVCF
      this.variant_index_type = org.broadinstitute.gatk.utils.variant.GATKVCFIndexType.LINEAR
      this.variant_index_parameter = 128000
      if(perBase)
        this.emitRefConfidence = org.broadinstitute.gatk.tools.walkers.haplotypecaller.ReferenceConfidenceMode.BP_RESOLUTION
    }
    this.analysisName = "HaplotypeCaller"
    this.stand_call_conf = qscript.stand_call_conf
    this.stand_emit_conf = qscript.stand_emit_conf
    this.isIntermediate = false
  }
}

