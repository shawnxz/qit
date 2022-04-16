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

package qit.data.utils.mri.fields;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import qit.data.datasets.Vect;
import qit.data.models.Fibers;
import qit.data.models.VectModel;
import qit.data.modules.mri.spharm.VolumeSpharmPeaks;
import qit.data.source.VectSource;
import qit.data.utils.mri.estimation.VolumeKernelModelEstimator;
import qit.data.utils.mri.structs.StreamlineField;
import qit.math.structs.VectFunction;

import java.util.List;
import java.util.Set;

public class SpharmPeakField extends StreamlineField
{
    private VectFunction peaker;
    private VolumeKernelModelEstimator estimator;

    public SpharmPeakField(VolumeKernelModelEstimator sampler, int dim)
    {
        this.peaker = new VolumeSpharmPeaks().factory(dim).get();
        this.estimator = sampler;
    }

    public List<StreamSample> getLines(Vect pos, Vect ref)
    {
        List<StreamSample> lines = Lists.newArrayList();

        Vect estimate = this.estimator.estimate(pos, ref);

        if (estimate != null)
        {
            Fibers model = new Fibers(this.peaker.apply(estimate));
            for (int idx = 0; idx < model.size(); idx++)
            {
                lines.add(new PeakSample(pos, model.getLine(idx), model.getFrac(idx)));
            }
        }
        return lines;
    }

    public List<StreamSample> getSamples(Vect pos)
    {
        return this.getLines(pos, null);
    }

    public String getAttr()
    {
        return VectModel.AMP;
    }

    public class PeakSample extends StreamSample
    {
        double amp;

        public PeakSample(Vect pos, Vect orient, double amp)
        {
            super(pos, orient, amp);
            this.amp = amp;
        }

        public Set<String> getAttrs()
        {
            Set<String> out = Sets.newHashSet();
            out.add(VectModel.AMP);
            return out;
        }

        public Vect getAttr(String name)
        {
            if (VectModel.AMP.equals(name))
            {
                return VectSource.create1D(this.amp);
            }

            throw new RuntimeException("undefined attribute: " + name);
        }
    }
}
