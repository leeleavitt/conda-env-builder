package com.github.nh13.condaenvbuilder.io

import cats.syntax.either._
import com.fulcrumgenomics.commons.CommonsDef.SafelyClosable
import com.fulcrumgenomics.commons.io.Io
import com.github.nh13.condaenvbuilder.CondaEnvironmentBuilderDef.PathToYaml
import com.github.nh13.condaenvbuilder.api.{Decoders, Spec}
import io.circe.{Error, yaml}


/** Read/parse methods for a conda environment specification (see [[Spec]]) */
object SpecParser {
  /** Reads a [[Spec]] from a YAML file.
    *
    * @param config the input YAML file
    * @throws Error if the input could not be parsed
    */
  def apply(config: PathToYaml): Spec = this.get(config).valueOr(throw _)

  /**
    *
    * @param config the input YAML file
    * @return either the error if the input could not be parsed, or the parsed [[Spec]]
    */
  def get(config: PathToYaml): Either[Error, Spec] = {
    import Decoders.DecodeSpec
    val reader = Io.toReader(config)
    val json   = yaml.parser.parse(reader)
    reader.safelyClose()
    json.flatMap(_.as[Spec])
  }
}