/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.cli;

import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.solr.common.cloud.SolrZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkCpTool extends ToolBase {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public ZkCpTool() {
    this(CLIO.getOutStream());
  }

  public ZkCpTool(PrintStream stdout) {
    super(stdout);
  }

  @Override
  public List<Option> getOptions() {
    return List.of(
        Option.builder("src")
            .argName("src")
            .hasArg()
            .required(true)
            .desc("Source file or directory, may be local or a Znode.")
            .build(),
        Option.builder("dst")
            .argName("dst")
            .hasArg()
            .required(true)
            .desc("Destination of copy, may be local or a Znode.")
            .build(),
        SolrCLI.OPTION_RECURSE,
        SolrCLI.OPTION_ZKHOST,
        SolrCLI.OPTION_VERBOSE);
  }

  @Override
  public String getName() {
    return "cp";
  }

  @Override
  public void runImpl(CommandLine cli) throws Exception {
    SolrCLI.raiseLogLevelUnlessVerbose(cli);
    String zkHost = SolrCLI.getZkHost(cli);
    if (zkHost == null) {
      throw new IllegalStateException(
          "Solr at "
              + cli.getOptionValue("solrUrl")
              + " is running in standalone server mode, cp can only be used when running in SolrCloud mode.\n");
    }

    try (SolrZkClient zkClient =
        new SolrZkClient.Builder()
            .withUrl(zkHost)
            .withTimeout(30000, TimeUnit.MILLISECONDS)
            .build()) {
      echoIfVerbose("\nConnecting to ZooKeeper at " + zkHost + " ...", cli);
      String src = cli.getOptionValue("src");
      String dst = cli.getOptionValue("dst");
      Boolean recurse = Boolean.parseBoolean(cli.getOptionValue("recurse"));
      echo("Copying from '" + src + "' to '" + dst + "'. ZooKeeper at " + zkHost);

      boolean srcIsZk = src.toLowerCase(Locale.ROOT).startsWith("zk:");
      boolean dstIsZk = dst.toLowerCase(Locale.ROOT).startsWith("zk:");

      String srcName = src;
      if (srcIsZk) {
        srcName = src.substring(3);
      } else if (srcName.toLowerCase(Locale.ROOT).startsWith("file:")) {
        srcName = srcName.substring(5);
      }

      String dstName = dst;
      if (dstIsZk) {
        dstName = dst.substring(3);
      } else {
        if (dstName.toLowerCase(Locale.ROOT).startsWith("file:")) {
          dstName = dstName.substring(5);
        }
      }
      zkClient.zkTransfer(srcName, srcIsZk, dstName, dstIsZk, recurse);
    } catch (Exception e) {
      log.error("Could not complete the zk operation for reason: ", e);
      throw (e);
    }
  }
}
