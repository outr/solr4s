package com.outr.solr4s.admin

import io.youi.client.HttpClient

case class SolrUpdateCommand(updateClient: HttpClient,
                             instructions: List[SolrUpdateInstruction] = Nil) extends UpdateInterface {
  override def withInstruction(instruction: SolrUpdateInstruction): UpdateInterface = {
    copy(instructions = instructions ::: List(instruction))
  }
}
