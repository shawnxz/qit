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

import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;

import java.util.List;

@ModuleDescription("Smooth out irregularities in the input curves using laplacian (or Taubin) smoothing.  This works best if curve vertices are equally spaced along each curve.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSmooth implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("the attribute to smooth")
    public String attr = Curves.COORD;

    @ModuleParameter
    @ModuleDescription("number of iterations")
    public int iters = 1;

    @ModuleParameter
    @ModuleDescription("lambda smoothing parameter")
    public double lambda = 0.3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("mu smoothing parameter (for Taubin smoothing)")
    public Double mu = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesSmooth run()
    {
        Curves curves = this.inplace ? this.input : this.input.copy();

        for (int i = 0; i < this.iters; i++)
        {
            smoothInPlace(curves, this.attr, 1, this.lambda);

            if (this.mu != null)
            {
                smoothInPlace(curves, this.attr, 1, -1.0 * this.mu);
            }
        }
        
        this.output = curves;
        return this;
    }

    public static void smoothInPlace(Curves curves, String attrs, int num, double lambda)
    {
        for (int iter = 0; iter < num; iter++)
        {
            for (String attr : attrs.split(","))
            {
                for (Curves.Curve curve : curves)
                {
                    for (int i = 1; i < curve.size() - 1; i++)
                    {
                        Vect prev = curve.get(attr, i - 1);
                        Vect curr = curve.get(attr, i);
                        Vect next = curve.get(attr, i + 1);

                        Vect dp = prev.minus(curr);
                        Vect dn = next.minus(curr);
                        Vect dc = dp.plus(dn).times(0.5);

                        Vect smooth = curr.plus(lambda, dc);

                        curve.set(attr, i, smooth);
                    }
                }
            }
        }
    }

    public static Curves apply(Curves curves)
    {
        return new CurvesSmooth()
        {{
            this.input = curves;
        }}.run().output;
    }
}