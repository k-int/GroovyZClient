//
// Generated from archetype; please customize.
//

package com.k_int.zcompare

import groovy.util.GroovyTestCase
import org.jzkit.z3950.gen.v3.Z39_50_APDU_1995.*

/**
 * Tests for the {@link Example} class.
 */
class ZReportTest extends GroovyTestCase {

    void testShow() {

        def config_file = "${java.lang.System.properties.'user.home'}/.groovy/zcomparetest.groovy"
        println "Attempting to load ${config_file}"

        def config = new ConfigSlurper().parse(new File(config_file).toURL())

        def comparisons = [
          "someserverpair" : [ [ host:"server1.host.com", port:210, authParams:[method:"userpass", user:config.user, pass:config.pass ] ],
                               [ host:"server2.host.com", port:210, authParams:[method:"userpass", user:config.new_user, pass:config.new_pass ] ] ] //,
          // "someserverpair" : [ [ host:"server1.host.com", port:210, authParams:[method:"userpass", user:config.user, pass:config.pass ] ],
          //                      [ host:"server2.host.com", port:210, authParams:[method:"userpass", user:config.new_user, pass:config.new_pass ] ] ] //,
        ];

        def testsuite = [
          "4"    : [ name:"Title",                    testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "5"    : [ name:"Title-Series",             testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "7"    : [ name:"ISBN",                     testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "8"    : [ name:"ISSN",                     testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "9"    : [ name:"LCCard",                   testdata: [ "Brain", "Image", "KerFlooble" ] ],
          // "12"   : [ name:"Local Number",             testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "16"   : [ name:"LC Call",                  testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "20"   : [ name:"Local Classn",             testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "21"   : [ name:"Subject Heading",          testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "51"   : [ name:"No. Music Pub.",           testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "60"   : [ name:"CODEN",                    testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "63"   : [ name:"Note",                     testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "1003" : [ name:"Author",                   testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "1004" : [ name:"Author Name - Personal",   testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "1005" : [ name:"Author Name - Corporate",  testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "1006" : [ name:"Author Name - Conf",       testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "1007" : [ name:"Identifier-Standard",      testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "1015" : [ name:"Concept-Reference",        testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "1016" : [ name:"Any",                      testdata: [ "Brain", "Image", "KerFlooble" ] ],
          "1032" : [ name:"Doc-Id",                   testdata: [ "Brain", "Image", "KerFlooble" ] ] ];

        def results = [:]

        println "Config.user : ${config.user}"

        // For each set of comparisons to run
        comparisons.each { servers_to_compare ->
          println "Running comparison set ${servers_to_compare.key}"
          servers_to_compare.value.each { server ->
            println "Run testsuite against ${server.host}:${server.port}"
            def client = new ZClient(server)
            try {
              client.connect()
              testsuite.each { test ->
                def searchterm = "brain"
                // println "Searching target using access point ${test.key} (${test.value.name})"
                SearchResponse_type sr = client.sendSearch(new org.jzkit.search.util.QueryModel.PrefixString.PrefixString("@attrset bib-1 @attr 1=${test.key} \"${searchterm}\""),
                                                           null,      // refid
                                                           "default", // setname
                                                           "b",
                                                           "usmarc",
                                                           ["ADVANCE"])

                println "Log test results..."
                if ( results["${test.key}:${searchterm}"] == null )
                  results["${test.key}:${searchterm}"] = [:];
                def r = results["${test.key}:${searchterm}"]

                if ( sr.searchStatus == true ) {
                  // println "test,${i},OK Response for @attr 1=${i} : ${sr.dump()}"
                  println "${servers_to_compare.key},${server.host}:${server.port},bib-1:1:${test.key},${test.value.name},OK,${sr.resultCount}"
                  r["${server.host}:${server.port}"] = "OK ${sr.resultCount}"
                }
                else {
                  println "${servers_to_compare.key},${server.host}:${server.port},bib-1:1:${test.key},${test.value.name},FAIL,${sr.resultCount}"
                  r["${server.host}:${server.port}"] = "FAIL"
                }
                Thread.sleep(100);
              }
              client.disconnect()
            }
            catch ( org.jzkit.z3950.Z3950Exception diag ) {
              println "test,bib-1:1:${i},EXCEPTION,${sr.resultCount}"
              diag.printStackTrace();
            }
          }
        }

        println "Test completed...."

        results.each { testresult ->
          print  "Test results for ${testresult.key} : "
          testresult.value.each { sr ->
            print "${sr.value} "
          }
          println ""
        }
    }
}
