/*******************************************************************************
  *
  * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
  * All rights reserved.
  *
  * The Software remains the property of Ryan Cabeen ("the Author").
  *
  * The Software is distributed "AS IS" under this Licence solely for
  * non-commercial use in the hope that it will be useful, but in order
  * that the Author as a charitable foundation protects its assets for
  * the benefit of its educational and research purposes, the Author
  * makes clear that no condition is made or to be implied, nor is any
  * warranty given or to be implied, as to the accuracy of the Software,
  * or that it will be suitable for any particular purpose or for use
  * under any specific conditions. Furthermore, the Author disclaims
  * all responsibility for the use which is made of the Software. It
  * further disclaims any liability for the outcomes arising from using
  * the Software.
  *
  * The Licensee agrees to indemnify the Author and hold the
  * Author harmless from and against any and all claims, damages and
  * liabilities asserted by third parties (including claims for
  * negligence) which arise directly or indirectly from the use of the
  * Software or the sale of any products based on the Software.
  *
  * No part of the Software may be reproduced, modified, transmitted or
  * transferred in any form or by any means, electronic or mechanical,
  * without the express permission of the Author. The permission of
  * the Author is not required if the said reproduction, modification,
  * transmission or transference is done without financial return, the
  * conditions of this Licence are imposed upon the receiver of the
  * product, and all original and amended source code is included in any
  * transmitted product. You may be held legally responsible for any
  * copyright infringement that is caused or encouraged by your failure to
  * abide by these terms and conditions.
  *
  * You are not permitted under this Licence to use this Software
  * commercially. Use for which any financial return is received shall be
  * defined as commercial use, and includes (1) integration of all or part
  * of the source code or the Software into a product for sale or license
  * by or on behalf of Licensee to third parties or (2) use of the
  * Software or any derivative of it for research with the final aim of
  * developing software products for sale or license to a third party or
  * (3) use of the Software or any derivative of it for research with the
  * final aim of developing non-software products for sale or license to a
  * third party, or (4) use of the Software to provide any service to an
  * external organisation for which payment is received.
  *
  ******************************************************************************/

package qit.main;

import com.google.common.collect.Lists;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliValues;
import qit.data.datasets.Mask;
import qit.data.datasets.Volume;
import qit.data.utils.volume.VolumeOnlineAxialStats;

import java.util.List;

public class VolumeAxialFuse implements CliMain
{
    public static void main(String[] args)
    {
        new VolumeAxialFuse().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "fuse volumes";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<Volume(s)>").withDoc("the input volumes").withNoMax());
            cli.withOption(new CliOption().asInput().asOptional().withName("mask").withArg("<Mask>").withDoc("specify a mask"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("spherical").withDoc("use input from spherical coordinates"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-mean").withArg("<Volume>").withDoc("specify the output mean volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-lambda").withArg("<Volume>").withDoc("specify the output lambda volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-coherence").withArg("<Volume>").withDoc("specify the output coherence volume"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");

            VolumeOnlineAxialStats fuser = new VolumeOnlineAxialStats();

            if (entries.keyed.containsKey("mask"))
            {
                String fn = entries.keyed.get("mask").get(0);
                Logging.info("using mask: " + fn);
                Mask mask = Mask.read(entries.keyed.get("mask").get(0));
                fuser.withMask(mask);
            }

            if (entries.keyed.containsKey("input"))
            {
                if (entries.keyed.containsKey("spherical"))
                {
                    List<String> sphericalFns = entries.keyed.get("input");
                    Global.assume(sphericalFns.size() == 2, "expected a theta/phi volume pair");
                    Logging.info("using spherical coordinate input");
                    String thetasFn = sphericalFns.get(0);
                    String phisFn = sphericalFns.get(1);

                    Logging.info("reading thetas: " + thetasFn);
                    Volume thetas = Volume.read(thetasFn);

                    Logging.info("reading phis: " + phisFn);
                    Volume phis = Volume.read(phisFn);

                    Logging.info("updating statistics");
                    fuser.update(thetas, phis);
                }
                else
                {
                    List<String> inputFns = entries.keyed.get("input");
                    Logging.info(String.format("using %d input axial volumes", inputFns.size()));
                    for (String fn : inputFns)
                    {
                        Logging.info("reading: " + fn);
                        Volume vol = Volume.read(fn);

                        Logging.info("updating statistics");
                        fuser.update(vol);
                    }
                }
            }

            Logging.info("compiling results");
            fuser.compile();

            Logging.info("writing output");
            if (entries.keyed.containsKey("output-mean"))
            {
                String fn = entries.keyed.get("output-mean").get(0);
                Logging.info("writing mean: " + fn);
                fuser.getOutputMean().write(fn);
            }

            if (entries.keyed.containsKey("output-lambda"))
            {
                String fn = entries.keyed.get("output-lambda").get(0);
                Logging.info("writing lambda: " + fn);
                fuser.getOutputLambda().write(fn);
            }

            if (entries.keyed.containsKey("output-coherence"))
            {
                String fn = entries.keyed.get("output-coherence").get(0);
                Logging.info("writing coherence: " + fn);
                fuser.getOutputCoherence().write(fn);
            }

            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}