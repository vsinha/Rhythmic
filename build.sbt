name := "Rhythmic"

version := "1.0"

scalaVersion := "2.11.5"

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)