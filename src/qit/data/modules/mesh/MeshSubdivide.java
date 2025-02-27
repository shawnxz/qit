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

package qit.data.modules.mesh;

import com.google.common.collect.Maps;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.AttrMap;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.HalfEdgePoly;
import qit.math.structs.Vertex;

import java.util.Map;

@ModuleDescription("Subdivide a mesh.  Each triangle is split into four with new vertices that split each edge in two.")
@ModuleAuthor("Ryan Cabeen")
public class MeshSubdivide implements Module
{
    @ModuleInput
    @ModuleDescription("the input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("a number of subdivisions")
    public int num = 1;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshSubdivide run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();

        for (int i = 0; i < this.num; i++)
        {
            subdivide(mesh);
        }

        this.output = mesh;

        return this;
    }

    public static void subdivide(Mesh mesh)
    {
        Map<Edge, Vertex> nedges = Maps.newHashMap();

        AttrMap<Vertex> nvattr = new AttrMap<>();
        for (Edge edge : mesh.graph.edges())
        {
            Vertex nvert = mesh.graph.addVertex();
            nedges.put(edge, nvert);

            Vect va = mesh.vattr.get(edge.getA(), Mesh.COORD);
            Vect vb = mesh.vattr.get(edge.getB(), Mesh.COORD);
            Vect vn = va.copy();
            vn.timesEquals(0.5);
            vn.plusEquals(0.5, vb);

            nvattr.set(nvert, Mesh.COORD, vn);
            nvattr.set(edge.getA(), Mesh.COORD, va);
            nvattr.set(edge.getB(), Mesh.COORD, vb);
        }

        HalfEdgePoly ngraph = mesh.graph.proto();
        for (Face face : mesh.graph.faces())
        {
            Vertex va = face.getA();
            Vertex vb = face.getB();
            Vertex vc = face.getC();

            Vertex vab = nedges.get(new Edge(va, vb));
            Vertex vbc = nedges.get(new Edge(vb, vc));
            Vertex vca = nedges.get(new Edge(vc, va));

            Face f0 = new Face(va, vab, vca);
            Face f1 = new Face(vab, vb, vbc);
            Face f2 = new Face(vbc, vc, vca);
            Face f3 = new Face(vab, vbc, vca);

            ngraph.add(f0);
            ngraph.add(f1);
            ngraph.add(f2);
            ngraph.add(f3);
        }

        mesh.vattr = nvattr;
        mesh.graph = ngraph;
    }

    public static Mesh apply(Mesh mesh)
    {
        Mesh out = mesh.copy();
        subdivide(out);
        return out;
    }
}
