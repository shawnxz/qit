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

package qit.data.modules.volume;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.utils.volume.VolumeFilter;

@ModuleDescription("Filter a volume using a Triangle kernel")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFilterTriangle implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("filter support (filter will be 2 * support + 1 in each dimension)")
    public int support = 3;

    @ModuleParameter
    @ModuleDescription("number of applications")
    public int num = 1;

    @ModuleParameter
    @ModuleDescription("preserve un-masked values through the filter")
    public boolean preserve;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the volume channel (default applies to all)")
    public Integer channel;

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Volume output;

    public VolumeFilterTriangle run()
    {
        Volume volume = this.input;

        VolumeFilter filter = new VolumeFilter();
        filter.withFilter(VolumeSource.triangle(2 * this.support + 1));
        filter.withNormalize(true);
        filter.withBoundary(true);
        filter.withThreads(this.threads);

        if (this.channel != null)
        {
            filter.withChannel(this.channel);
        }

        for (int i = 0; i < this.num; i++)
        {
            volume = filter.withInput(volume).withMask(this.mask).withPass(this.preserve).run().getOutput();
        }

        this.output = volume;

        return this;
    }
}
