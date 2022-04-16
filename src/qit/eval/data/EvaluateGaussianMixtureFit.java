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


package qit.eval.data;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Matrix;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.structs.Gaussian;
import qit.math.structs.GaussianMixture;
import qit.math.utils.MathUtils;
import smile.stat.distribution.MultivariateGaussianMixture;

@ModuleDescription("This program evaluates Gaussian Mixture distribution parameter estimation with a simulation")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class EvaluateGaussianMixtureFit implements Module
{
    @ModuleParameter
    @ModuleDescription("the number of repetitions")
    public int reps = 100;

    @ModuleParameter
    @ModuleDescription("the minimum number of samples")
    public int min = 100;

    @ModuleParameter
    @ModuleDescription("the number of steps")
    public int steps = 20;

    @ModuleParameter
    @ModuleDescription("the maximum number of samples")
    public int max = 5000;

    @ModuleOutput
    @ModuleDescription("output table of results")
    public Table output;

    @Override
    public EvaluateGaussianMixtureFit run()
    {
        Table table = new Table();
        table.withField("repetition");
        table.withField("samples");
        table.withField("divLeft");
        table.withField("divRight");
        table.withField("divSym");

        double[] weights = {0.2, 0.1, 0.5, 0.2};
        Gaussian[] comps = new Gaussian[4];

        for (int i = 0; i < 4; i++)
        {
            Vect mean = VectSource.create3D(1000.5 * Math.random(), 500.0 * Math.random(), 250.0 * Math.random());
            Matrix L = new Matrix(3, 3);
            L.set(0, 0, 7.0 * Math.random());
            L.set(1, 1, 4.2 * Math.random());
            L.set(2, 2, 3.5 * Math.random());
            Matrix cov = L.times(L.transpose());

            comps[i] = new Gaussian(mean, cov);
        }

        GaussianMixture model = new GaussianMixture(weights, comps);

        for (int i = 0; i < this.steps; i++)
        {
            int samples = MathUtils.round(this.min + (this.max - this.min) * i / (double) (this.steps - 1));

            for (int rep = 0; rep < this.reps; rep++)
            {
                Vects points = new Vects();

                for (int sample = 0; sample < samples; sample++)
                {
                    points.add(model.sample());
                }

                MultivariateGaussianMixture smile = new MultivariateGaussianMixture(points.toNumDimArray(), 4, true);
                double[] weightsFit = new double[4];
                Gaussian[] compsFit = new Gaussian[4];

                for (int k = 0; k < 4; k++)
                {
                    MultivariateGaussianMixture.Component comp = smile.getComponents().get(k);
                    double w = comp.priori;
                    double[] mean = comp.distribution.mean();
                    double[][] cov = comp.distribution.cov();

                    weightsFit[k] = w;
                    compsFit[k] = new Gaussian(new Vect(mean), new Matrix(cov));
                }

                GaussianMixture estimated = new GaussianMixture(weightsFit, compsFit);

//                VectsClusterKM initter = new VectsClusterKM();
//                initter.withK(4);
//                initter.withVects(points);
//                int[] init = initter.getOutput();
//
//                VectsClusterGM cluster = new VectsClusterGM();
//                cluster.withVects(points);
//                cluster.withWeights(VectSource.createND(points.size(), 1.0));
//                cluster.withK(4);
//                cluster.withType(CovarianceType.diagonal);
//                cluster.init(init);
//                cluster.run();
//                GaussianMixture estimated = cluster.getModel();

                try
                {

                    double divLeft = model.div(estimated);
                    double divRight = estimated.div(model);
                    double divSym = model.divsym(estimated);

                    if (!Double.isNaN(divSym))
                    {
                        Record rec = new Record();
                        rec.with("repetition", rep);
                        rec.with("samples", samples);
                        rec.with("divLeft", divLeft);
                        rec.with("divRight", divRight);
                        rec.with("divSym", divSym);
                        table.addRecord(rec);
                    }
                }
                catch (Exception e)
                {
                    Logging.info("warning: found singular solution");
                }
            }
        }

        this.output = table;

        return this;
    }
}
