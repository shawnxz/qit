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

package qit.data.modules.mri.fibers;

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.models.Fibers;

@ModuleUnlisted
@ModuleDescription("Create a phantom demonstrating biases when visualizing fibers on grids")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersPhantomGridding implements Module
{
    @ModuleParameter
    @ModuleDescription("the width in voxels")
    public int width = 20;

    @ModuleParameter
    @ModuleDescription("the height in voxels")
    public int height = 20;

    @ModuleParameter
    @ModuleDescription("the number of slices")
    public int slices = 1;

    @ModuleOutput
    @ModuleDescription("the output fibers volume")
    public Volume output;

    public VolumeFibersPhantomGridding run()
    {
        Global.assume(this.width > 0 && this.height > 0 && this.slices > 0, "invalid dimensions");
        Sampling sampling = SamplingSource.create(this.width, this.height, this.slices);
        Volume out = VolumeSource.create(sampling, new Fibers(1).getEncodingSize());

        for (Sample sample : sampling)
        {
            int si = sample.getI();
            int sj = sample.getJ();

            double fi = si / (double) this.width;
            double fj = sj / (double) this.height;

            Vect dir = null;
            if (fi >= 0.75 && fi <= 1)
            {
                // vertical bundle

                dir = VectSource.create3D(0, 1, 0);
            }
            else if ((fj >= 1 - fi) && (fj <= 1 - fi + 0.353))
            {
                // 45 degree bundle
                dir = VectSource.create3D(-1, 1, 0).normalize();
            }
            else if ((fj <= 0.63 - 0.466 * fi) && (fj >= 0.63 - 0.466 * (fi + 0.6)))
            {
                // 22.5 degree bundle
                dir = VectSource.create3D(1, -0.466, 0).normalize();
            }

            if (dir != null)
            {
                Fibers fibers = new Fibers(1);
                fibers.setFrac(0, 1.0);
                fibers.setLine(0, dir);

                out.set(sample, fibers.getEncoding());
            }
        }

        this.output = out;

        return this;
    }
}
