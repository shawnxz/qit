/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.modules.mri.noddi;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.models.Noddi;
import qit.data.utils.mri.estimation.NoddiEstimator;

import java.util.List;

@ModuleDescription("Smooth a noddi volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeNoddiSmooth implements Module
{
    @ModuleInput
    @ModuleDescription("the input Noddi volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the filter radius in voxels")
    public Integer support = 3;

    @ModuleParameter
    @ModuleDescription("the positional bandwidth in mm")
    public Double hpos = 1.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the directionally adaptive bandwidth (only used with adaptive flag)")
    public Double hdir = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the baseline signal adaptive bandwidth")
    public Double hsig = null;

    @ModuleParameter
    @ModuleDescription("specify an estimation method")
    public String estimation = NoddiEstimator.SCATTER;

    @ModuleOutput
    @ModuleDescription("the output noddi volume")
    public Volume output;

    public VolumeNoddiSmooth run()
    {
        Global.assume(this.input != null, "input fibers not found");

        int n = this.support;
        Volume filter = VolumeSource.gauss(this.input.getSampling(), n, n ,n, this.hpos);
        Sampling sampling = this.input.getSampling();
        Sampling fsampling = filter.getSampling();
        Volume out = this.input.proto();

        int cx = (fsampling.numI() - 1) / 2;
        int cy = (fsampling.numJ() - 1) / 2;
        int cz = (fsampling.numK() - 1) / 2;

        int size = sampling.size();
        int step = Math.max(1, size / 50);

        NoddiEstimator estimator = new NoddiEstimator();
        estimator.parse(this.estimation);

        Logging.progress("filtering");
        for (Sample sample : sampling)
        {
            int idx = sampling.index(sample);
            if (idx % step == 0)
            {
                Logging.progress(String.format("%d percent processed", 100 * idx / (size - 1)));
            }

            if (!this.input.valid(sample, this.mask))
            {
                continue;
            }

            Noddi source = new Noddi(this.input.get(sample));

            List<Double> weights = Lists.newArrayList();
            List<Vect> models = Lists.newArrayList();
            for (Sample fsample : fsampling)
            {
                int ni = sample.getI() + fsample.getI() - cx;
                int nj = sample.getJ() + fsample.getJ() - cy;
                int nk = sample.getK() + fsample.getK() - cz;
                Sample nsample = new Sample(ni, nj, nk);

                if (!this.input.valid(nsample, this.mask))
                {
                    continue;
                }

                Noddi nmodel = new Noddi(this.input.get(nsample));
                double weight = filter.get(fsample, 0);

                if (this.hdir != null)
                {
                    double d = nmodel.dist(source);
                    double d2 = d * d;
                    double h2 = this.hdir * this.hdir;
                    double kern = Math.exp(-d2 / h2);
                    weight *= kern;
                }

                if (this.hsig != null)
                {
                    double db = nmodel.getBaseline() - source.getBaseline();
                    double dist2 = db * db;
                    double h = this.hsig;
                    double h2 = h * h;
                    double kern = Math.exp(-dist2 / h2);
                    weight *= kern;
                }

                models.add(nmodel.getEncoding());
                weights.add(weight);
            }

            Vect est = estimator.run(weights, models);

            if (est != null)
            {
                out.set(sample, est);
            }
        }

        this.output = out;
        return this;
    }
}
