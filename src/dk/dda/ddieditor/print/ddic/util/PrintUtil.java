package dk.dda.ddieditor.print.ddic.util;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import org.ddialliance.ddieditor.ui.util.LanguageUtil;

public class PrintUtil extends org.ddialliance.ddieditor.ui.util.PrintUtil {
	final String ddiLToDdiDLocation = "resources/ddixslt-lc/ddi3_1_to_ddi1_2_2.xsl";
	final String ddiCToFoLocation = "resources/ddixslt-cpdf/dditofo_complete.xsl";
	
	public Transformer getDdiLToDdiCTransformer() throws Exception {
		Source ddiLToDdiCLocationXslt = getXsltSource(ddiLToDdiDLocation);
		Transformer transformer = getTransformer(ddiLToDdiCLocationXslt);
		
		// setup transformer
		 transformer.setParameter("multilang", "false");
		 // TODO -- set missing transformer params
	     transformer.setParameter("lang", LanguageUtil.getDisplayLanguage());
	     transformer.setParameter("identification-prefix", "DDA");
	     transformer.setParameter("distributionuri", "http://localhost/catalogue/");
	     transformer.setParameter("translations", "i18n/messages_"
	                + LanguageUtil.getDisplayLanguage() + ".properties.xml");
//	     transformer.setParameter("createVarGoup", this.isCreateVariableGroups ? "true" : "false");
//	     transformer.setParameter("createDefaultSystemMissing", this.isAddSystemMissing ? "true" : "false");
	     transformer.setParameter("createVarGoup", "false");
	     transformer.setParameter("createDefaultSystemMissing", "false");
	     transformer.setParameter("prefixVarLableWithVarname", "false");
	        
		return transformer;
	}
	
	public Transformer getDdiCToFoTransformer() throws Exception {
		Source ddiLToDdiDLocationXslt = getXsltSource(ddiCToFoLocation);
		Transformer transformer = getTransformer(ddiLToDdiDLocationXslt);

		// setup transformer
		// TODO -- set missing transformer params

		return transformer;
	}

}
