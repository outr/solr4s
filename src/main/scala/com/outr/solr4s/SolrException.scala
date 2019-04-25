package com.outr.solr4s

import com.outr.solr4s.admin.GeneralResponse

case class SolrException(response: GeneralResponse,
                         cause: Option[Throwable] = None) extends RuntimeException(response.message, cause.orNull)