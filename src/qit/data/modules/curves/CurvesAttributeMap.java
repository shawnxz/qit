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

package qit.data.modules.curves;

import qit.base.Global;
import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.models.Tensor;
import qit.data.modules.mask.MaskDilate;
import qit.data.modules.mri.fibers.VolumeFibersSmooth;
import qit.data.modules.volume.VolumeEnhanceContrast;
import qit.data.source.MatrixSource;
import qit.data.source.VolumeSource;
import qit.data.utils.MatrixUtils;
import qit.math.utils.MathUtils;

@ModuleDescription("Compute an attribute map, that is, a volume that represents the most likely value of a given curves attribute for each voxel.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesAttributeMap implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference volume (exclusive with refmask)")
    public Volume refvolume;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference mask (exclusive with refvolume)")
    public Mask refmask;

    @ModuleParameter
    @ModuleDescription("the name of attribute to extract from the curves")
    public String attr = "attr";

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output density map")
    public Volume outputDensity;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output mask")
    public Mask outputMask;

    @ModuleOutput
    @ModuleDescription("output attribute map")
    public Volume output;

    @Override
    public CurvesAttributeMap run()
    {
        Global.assume(this.refmask != null ^ this.refvolume != null, "only refmask or refvolume must be specified but not both");
        Sampling sampling = this.refmask == null ? this.refvolume.getSampling() : this.refmask.getSampling();

        Global.assume(this.input.has(this.attr), "attribute not found: " + this.attr);

        final Volume out = VolumeSource.create(sampling, this.input.dim(this.attr));
        final Volume counts = VolumeSource.create(sampling);

        Logging.info("traversing curves");
        for (Curves.Curve curve : this.input)
        {
            for (Pair<Sample, Vect> pair: sampling.traverseAttribute(curve.getAll(Curves.COORD), curve.getAll(this.attr)))
            {
                Sample sample = pair.a;
                Vect value = pair.b;

                if (sampling.contains(sample))
                {
                    double ncount = counts.get(sample).get(0) + 1;
                    counts.set(sample, ncount);

                    out.set(sample, out.get(sample).plus(value));
                }
            }
        }

        Mask mask = new Mask(sampling);

        for (Sample sample : sampling)
        {
            int count = (int) counts.get(sample, 0);
            Vect sum = out.get(sample);

            if (count > 0)
            {
                out.set(sample, sum.divSafe(count));
                mask.set(sample, 1);
            }
        }

        this.output = out;
        this.outputMask = mask;
        this.outputDensity = counts;

        return this;
    }
}