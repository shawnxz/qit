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
 * product, and all original and amended source code is retaind in any
 * transmitted product. You may be held legally responsible for any
 * copyright infringement that is caused or encouraged by your failure to
 * abide by these terms and conditions.
 *
 * You are not permitted under this Licence to use this Software
 * commercially. Use for which any financial return is received shall be
 * defined as commercial use, and retains (1) integration of all or part
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

package qit.data.modules.table;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;

import java.util.List;

@ModuleDescription("Print data from a table")
@ModuleAuthor("Ryan Cabeen")
public class TablePrintData implements Module
{
    @ModuleInput
    @ModuleDescription("the input table")
    private Table input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an predicate for selecting records (using existing field names)")
    public String where;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the name variable for use in the select predicate")
    public String name = "name";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("sort by fields (e.g. field2,#field3,^#field1).  '#' indicates the value is numeric, and '^' indicates the sorting should be reversed")
    public String sort;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("retain only specific fields (comma delimited)")
    public String retain;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("remove specific fields (comma delimited)")
    public String remove;

    @ModuleParameter
    @ModuleDescription("use tabs")
    public boolean tab = false;

    @ModuleParameter
    @ModuleDescription("print the header")
    public boolean header = false;

    @ModuleParameter
    @ModuleDescription("use the given string for missing values")
    public String na = "NA";

    @Override
    public Module run()
    {
        Table table = this.input;

        if (this.where != null)
        {
            table = TableUtils.where(table, this.where);
        }

        if (this.sort != null)
        {
            table = TableUtils.sort(table, this.sort);
        }

        if (this.remove != null)
        {
            table = TableUtils.remove(table, this.remove);
        }

        if (this.retain != null)
        {
            table = TableUtils.retain(table, this.retain);
        }

        char sep = this.tab ? '\t' : ' ';

        if (this.header)
        {
            List<String> tokens = Lists.newArrayList();
            for (String field : table.getFields())
            {
                tokens.add(field);
            }
            System.out.println(StringUtils.join(tokens, sep));
        }

        for (int key : table.keys())
        {
            Record record = table.getRecord(key);

            List<String> tokens = Lists.newArrayList();
            for (String field : table.getFields())
            {
                tokens.add(record.get(field));
            }
            System.out.println(StringUtils.join(tokens, sep));
        }

        return this;
    }
}
