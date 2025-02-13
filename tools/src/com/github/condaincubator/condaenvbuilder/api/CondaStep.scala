package com.github.condaincubator.condaenvbuilder.api

import CondaStep.Channel
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Specifies the conda channels and requirements for a conda environment.
  *
  * @param channels the conda channels, in priority order
  * @param requirements the package requirements.
  */
case class CondaStep(channels: Seq[Channel]=Seq.empty, requirements: Seq[Requirement]=Seq.empty) extends StepWithDefaults {

  /** Inherit (in-order) channels and requirements from the given step(s).
    *
    * Inherited channels are prioritized first.
    *
    * @param step one or more steps from which to inherit.
    */
  override def inheritFrom(step: Step*): CondaStep = {
    val steps = step.collect { case s: CondaStep => s }
    this.copy(
      channels     = (steps.flatMap(_.channels) ++ this.channels).distinct,
      requirements = Requirement.join(parent=steps.flatMap(_.requirements), child=requirements),
    )
  }

  /** Applies the default step to this step.  The default channels are appended to the current list of channels.  Any
    * requirement that has a default version is updated (and must be present in the default step).
    *
    * @param defaults the default step.
    */
  def withDefaults(defaults: Step): CondaStep = defaults match {
    case _defaults: CondaStep =>
      this.copy(
        channels     = (this.channels ++ _defaults.channels).distinct,
        requirements = Requirement.withDefaults(requirements=this.requirements, defaults=_defaults.requirements),
      )
    case _ => this
  }
}

/** Example YAML encoding for [[CondaStep]]
  * {{{
  *   - conda:
  *     channels:
  *       - conda-forge
  *       - bioconda
  *     requirements:
  *       - samtools
  *       - fgbio=1.1.0
  * }}}
  *
  * Both `channels` and `requirements` are optional.
  *
  * Requirements may omit version numbers, which can be compiled from the default environment
  */
object CondaStep {
  type Channel = String

  import Encoders.EncodeRequirement

  /** Returns an YAML encoder for [[CondaStep]] */
  def encoder: Encoder[CondaStep] = new Encoder[CondaStep] {
    final def apply(step: CondaStep): Json = Json.obj(
      ("channels", Json.fromValues(step.channels.map(_.asJson))),
      ("requirements", Json.fromValues(step.requirements.map(_.asJson)))
    )
  }

  /** Returns a YAML decoder for [[CondaStep]] */
  def decoder: Decoder[CondaStep] = new Decoder[CondaStep] {
    import Decoders.DecodeRequirement

    final def apply(c: HCursor): Decoder.Result[CondaStep] = {
      val keys: Seq[String] = c.keys.map(_.toSeq).getOrElse(Seq.empty)

      val channelsResults: Result[Seq[String]] = {
        if (keys.contains("channels")) c.downField("channels").as[Seq[String]]
        else Right(Seq.empty)
      }

      val requirementsResults: Result[Seq[Requirement]] = {
        if (keys.contains("requirements")) c.downField("requirements").as[Seq[Requirement]]
        else Right(Seq.empty)
      }

      for {
        channels <- channelsResults
        requirements <- requirementsResults
      } yield {
        CondaStep(channels=channels, requirements=requirements)
      }
    }
  }
}
