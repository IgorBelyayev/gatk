package org.broadinstitute.sting.queue.qscripts.DoC

import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.commandline.Hidden
import org.broadinstitute.sting.queue.util.VCF_BAM_utilities
import org.broadinstitute.sting.queue.util.DoC._
import org.broadinstitute.sting.gatk.walkers.coverage.CoverageUtils

class CalcDepthOfCoverage extends QScript {
  qscript =>

  @Input(doc = "bam input, as .bam or as a list of files", shortName = "I", required = true)
  var bams: File = _

  @Input(doc = "gatk jar file", shortName = "J", required = true)
  var gatkJarFile: File = _

  @Input(doc = "xhmm executable file", shortName = "xhmmExec", required = true)
  var xhmmExec: File = _

  @Input(shortName = "R", doc = "ref", required = true)
  var referenceFile: File = _

  @Input(shortName = "L", doc = "Intervals", required = false)
  var intervals: File = _

  @Argument(doc = "Expand the intervals by this number of bases (on each side)", shortName = "expandIntervals", required = false)
  var expandIntervals = 0

  @Argument(doc = "level of parallelism for BAM DoC.   By default is set to 0 [no scattering].", shortName = "scatter", required = false)
  var scatterCountInput = 0

  @Argument(doc = "Samples to run together for DoC.   By default is set to 1 [one job per sample].", shortName = "samplesPerJob", required = false)
  var samplesPerJob = 1

  @Output(doc = "Base name for files to output", shortName = "o", required = true)
  var outputBase: File = _

  @Hidden
  @Argument(doc = "How should overlapping reads from the same fragment be handled?", shortName = "countType", required = false)
  //
  // TODO: change this to be the default once UnifiedGenotyper output is annotated with *fragment-based depth* (and this is used for filtering):
  //var countType = CoverageUtils.CountPileupType.COUNT_FRAGMENTS_REQUIRE_SAME_BASE
  //
  var countType = CoverageUtils.CountPileupType.COUNT_READS

  @Argument(doc = "Maximum depth (before GATK down-sampling kicks in...)", shortName = "MAX_DEPTH", required = false)
  var MAX_DEPTH = 20000

  @Hidden
  @Argument(doc = "Number of read-depth bins", shortName = "NUM_BINS", required = false)
  var NUM_BINS = 200

  @Hidden
  @Argument(doc = "Starting value of read-depth bins", shortName = "START_BIN", required = false)
  var START_BIN = 1

  @Argument(doc = "Minimum read mapping quality", shortName = "MMQ", required = false)
  var minMappingQuality = 0

  @Argument(doc = "Minimum base quality to be counted in depth", shortName = "MBQ", required = false)
  var minBaseQuality = 0

  @Argument(doc = "Memory (in GB) required for storing the whole matrix in memory", shortName = "wholeMatrixMemory", required = false)
  var wholeMatrixMemory = -1

  @Argument(doc = "Memory (in GB) required for merging the big matrix", shortName = "largeMemory", required = false)
  var largeMemory = -1

  @Argument(shortName = "sampleIDsMap", doc = "File mapping BAM sample IDs to desired sample IDs", required = false)
  var sampleIDsMap: String = ""

  @Argument(shortName = "sampleIDsMapFromColumn", doc = "Column number of OLD sample IDs to map", required = false)
  var sampleIDsMapFromColumn = 1

  @Argument(shortName = "sampleIDsMapToColumn", doc = "Column number of NEW sample IDs to map", required = false)
  var sampleIDsMapToColumn = 2

  @Argument(shortName = "longJobQueue", doc = "Job queue to run the 'long-running' commands", required = false)
  var longJobQueue: String = ""

  @Argument(shortName = "minCoverageCalcs", doc = "Coverage thresholds against which to measure each sample", required = false)
  var minCoverageCalcs = List(10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60).toArray


  val PREPARED_TARGS_SUFFIX: String = ".merged.interval_list"

  val DOC_OUTPUT_SUFFIX: String = ".DoC.txt"
  val BASE_DOC_OUTPUT_SUFFIX: String = ".per_base.DoC.txt"


  trait LargeMemoryLimit extends CommandLineFunction {
    if (largeMemory < 0) {
      this.memoryLimit = 6
    }
    else {
      this.memoryLimit = largeMemory
    }
  }

  trait WholeMatrixMemoryLimit extends CommandLineFunction {
    // Since loading ALL of the data can take significant memory:
    if (wholeMatrixMemory < 0) {
      this.memoryLimit = 24
    }
    else {
      this.memoryLimit = wholeMatrixMemory
    }
  }

  trait LongRunTime extends CommandLineFunction {
    if (longJobQueue != "")
      this.jobQueue = longJobQueue
  }

  class extendFlanks(inIntervals: File, outIntervals: File) extends WriteFlankingIntervalsFunction {
      this.inputIntervals = inIntervals
      this.outputIntervals = outIntervals
      this.flankSize = qscript.expandIntervals
      this.reference = qscript.referenceFile
  }

  def script = {
    val prepTargetsInitial = new PrepareTargets(List(qscript.intervals), outputBase.getPath + PREPARED_TARGS_SUFFIX, xhmmExec, referenceFile)
    add(prepTargetsInitial)

    var prepTargets = prepTargetsInitial
    if (qscript.intervals != null && qscript.expandIntervals > 0) {
      val flankIntervalsFile = new File(outputBase.getPath + ".flank.interval_list")
      val extendCommand = new extendFlanks(prepTargetsInitial.out, flankIntervalsFile)
      add(extendCommand)

      prepTargets = new PrepareTargets(List(qscript.intervals, extendCommand.outputIntervals), outputBase.getPath + ".withFlanking" + PREPARED_TARGS_SUFFIX, xhmmExec, referenceFile)
      add(prepTargets)
    }

    trait CommandLineGATKArgs extends CommandLineGATK {
      this.intervals :+= prepTargets.out
      this.jarFile = qscript.gatkJarFile
      this.reference_sequence = qscript.referenceFile
      this.logging_level = "INFO"
    }

    val sampleToBams: scala.collection.mutable.Map[String, scala.collection.mutable.Set[File]] = VCF_BAM_utilities.getMapOfBAMsForSample(VCF_BAM_utilities.parseBAMsInput(bams))
    val samples: List[String] = sampleToBams.keys.toList
    Console.out.printf("Samples are %s%n", samples)

    val groups: List[Group] = buildDoCgroups(samples, sampleToBams, samplesPerJob, outputBase)
    var docs: List[DoCwithDepthOutputAtEachBase] = List[DoCwithDepthOutputAtEachBase]()
    for (group <- groups) {
      Console.out.printf("Group is %s%n", group)
      docs ::= new DoCwithDepthOutputAtEachBase(group.bams, group.DoC_output, countType, MAX_DEPTH, minMappingQuality, minBaseQuality, scatterCountInput, START_BIN, NUM_BINS, minCoverageCalcs) with CommandLineGATKArgs
    }
    addAll(docs)

    for (minCoverageCalc <- minCoverageCalcs) {
      val mergeDepths = new MergeGATKdepths(docs.map(u => u.intervalSampleOut), outputBase.getPath + ".min_" + minCoverageCalc + DOC_OUTPUT_SUFFIX, "_%_above_" + minCoverageCalc, xhmmExec, sampleIDsMap, sampleIDsMapFromColumn, sampleIDsMapToColumn, None, false) with WholeMatrixMemoryLimit
      add(mergeDepths)
    }

    // Want 0 precision for base counts:
    val mergeBaseDepths = new MergeGATKdepths(docs.map(u => u.outPrefix), outputBase.getPath + BASE_DOC_OUTPUT_SUFFIX, "Depth_for_", xhmmExec, sampleIDsMap, sampleIDsMapFromColumn, sampleIDsMapToColumn, Some(0), true) with LargeMemoryLimit with LongRunTime
    add(mergeBaseDepths)
  }
}
