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

package qit.data.utils.vects.stats;

import qit.base.Global;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;

public class VectsAxialStats
{
    public Vects input;
    public Vect weights;

    public Integer count;
    public Vect mean;
    public Double lambda;
    public Double coherence;

    public VectsAxialStats clear()
    {
        this.mean = null;
        this.coherence = null;
        this.lambda = null;
        this.count = null;

        return this;
    }

    public VectsAxialStats withInput(Vects vals)
    {
        this.input = vals;
        return this.clear();
    }

    public VectsAxialStats withWeights(Vect vals)
    {
        this.weights = vals;
        return this.clear();
    }

    public VectsAxialStats run()
    {
        Global.assume(this.input != null && this.input.size() > 0, "no input found");
        Global.assume(this.weights == null || this.weights.size() == this.input.size(), "no weights don't match input");

        int num = this.input.size();

        VectsOnlineAxialStats stats = new VectsOnlineAxialStats();
        for (int i = 0; i < num; i++)
        {
            double w = this.weights != null ? this.weights.get(i) : 1;
            stats.update(w, this.input.get(i));
        }

        this.mean = stats.mean;
        this.lambda = stats.lambda;
        this.coherence = stats.coherence;
        this.count = stats.count;

        return this;
    }
}
