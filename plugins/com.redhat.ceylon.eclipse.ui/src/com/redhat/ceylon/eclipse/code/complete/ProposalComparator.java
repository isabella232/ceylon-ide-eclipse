package com.redhat.ceylon.eclipse.code.complete;

import static com.redhat.ceylon.eclipse.util.Types.getResultType;
import static java.lang.Character.isUpperCase;

import java.util.Comparator;

import com.redhat.ceylon.model.typechecker.model.Declaration;
import com.redhat.ceylon.model.typechecker.model.DeclarationWithProximity;
import com.redhat.ceylon.model.typechecker.model.Type;

final class ProposalComparator 
        implements Comparator<DeclarationWithProximity> {
    private final String prefix;
    private final Type type;

    ProposalComparator(String prefix, Type type) {
        this.prefix = prefix;
        this.type = type;
    }

    public int compare(
            DeclarationWithProximity x, 
            DeclarationWithProximity y) {
        try {
            /*boolean xbt = x.getDeclaration() instanceof NothingType;
            boolean ybt = y.getDeclaration() instanceof NothingType;
            if (xbt&&ybt) {
                return 0;
            }
            if (xbt&&!ybt) {
                return 1;
            }
            if (ybt&&!xbt) {
                return -1;
            }*/
            String xName = x.getName();
            String yName = y.getName();
            boolean yUpperCase = 
                    isUpperCase(yName.codePointAt(0));
            boolean xUpperCase = 
                    isUpperCase(xName.codePointAt(0));
            if (!prefix.isEmpty()) {
                //proposals which match the case of the
                //typed prefix first
                boolean upperCasePrefix = 
                        isUpperCase(prefix.codePointAt(0));
                if (!xUpperCase && yUpperCase) {
                    return upperCasePrefix ? 1 : -1;
                }
                else if (xUpperCase && !yUpperCase) {
                    return upperCasePrefix ? -1 : 1;
                }
            }
            Declaration xd = x.getDeclaration();
            Declaration yd = y.getDeclaration();
            if (type!=null) {
                Type xtype = getResultType(xd);
                Type ytype = getResultType(yd);
                boolean xassigns = 
                        xtype!=null && 
                        xtype.isSubtypeOf(type);
                boolean yassigns = 
                        ytype!=null && 
                        ytype.isSubtypeOf(type);
                if (xassigns && !yassigns) {
                    return -1;
                }
                if (yassigns && !xassigns) {
                    return 1;
                }
                if (xassigns && yassigns) {
                    //both are assignable - prefer the
                    //one which isn't assignable to
                    //*everything*
                    boolean xbottom = 
                            xtype!=null && 
                            xtype.isNothing();
                    boolean ybottom = 
                            ytype!=null && 
                            ytype.isNothing();
                    if (xbottom && !ybottom) {
                        return 1;
                    }
                    if (ybottom && !xbottom) {
                        return -1;
                    }
                    /*boolean xtd = 
                            x.getDeclaration() 
                                instanceof TypedDeclaration;
                    boolean ytd = 
                            y.getDeclaration() 
                                instanceof TypedDeclaration;
                    if (xtd && !ytd) {
                        return -1;
                    }
                    if (ytd && !xtd) {
                        return 1;
                    }*/
                }
            }
            int pc = Integer.compare(x.getProximity(), 
                                     y.getProximity());
            if (pc!=0) {
                return pc;
            }
            //lowercase proposals first if no prefix
            if (!xUpperCase && yUpperCase) {
                return -1;
            }
            else if (xUpperCase && !yUpperCase) {
                return 1;
            }
            int nc = xName.compareTo(yName);
            if (nc!=0) {
                return nc;
            }
            String xqn = xd.getQualifiedNameString();
            String yqn = yd.getQualifiedNameString();
            return xqn.compareTo(yqn);
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}