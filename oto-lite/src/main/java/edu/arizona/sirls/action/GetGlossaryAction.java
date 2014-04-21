package edu.arizona.sirls.action;

/**
 * Get glossaries from db and ontology lookup
 * @author Fengqiong
 */

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.arizona.sirls.beans.TermGlossaryBean;
import edu.arizona.sirls.db.GeneralDBAccess;
import edu.arizona.sirls.db.OntologyDBAccess;
import edu.arizona.sirls.form.GeneralForm;
import edu.arizona.sirls.oto.QueryOTO;
import edu.arizona.sirls.util.Utilities;

public class GetGlossaryAction extends ParserAction {
	/** Getting the instance of logger. */
	private static final Logger LOGGER = Logger
			.getLogger(GetGlossaryAction.class);

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		try {

			GeneralForm gform = (GeneralForm) form;
			String term = gform.getValue();
			String uploadID = "1";
			int glossaryType = GeneralDBAccess.getInstance().getGlossaryType(
					Integer.parseInt(uploadID));

			response.setContentType("text/xml");
			StringBuffer responseString = new StringBuffer("<glossaries>");

			// get from PATO
			// OlsClient olsC = new OlsClient(term, "PATO");
			// if (olsC.hasData()) {
			// responseString.append("<glossary>");
			// responseString.append("<id>In 'PATO': " + olsC.getTermID() +
			// "</id>");
			// responseString.append("<category>In 'PATO': " + olsC.getParent()
			// + "</category>");
			// responseString.append("<definition>In 'PATO': "+
			// olsC.getDefinition() + "</definition>");
			// responseString.append("</glossary>");
			// }

			// get from OTO
			ArrayList<TermGlossaryBean> glosses = QueryOTO.getInstance()
					.getGlossaryInfo(term,
							Utilities.getGlossaryNameByID(glossaryType));
			for (TermGlossaryBean gloss : glosses) {
				responseString.append("<glossary>");
				responseString
						.append("<id>OTO ID : " + gloss.getId() + "</id>");
				responseString.append("<category>" + gloss.getCategory()
						+ "</category>");
				responseString.append("<definition>" + gloss.getDefinition()
						+ "</definition>");
				responseString.append("</glossary>");
			}

			// get from ontology match
			glosses = OntologyDBAccess.getInstance().getOntologyMatchForTerm(
					term, glossaryType);
			for (TermGlossaryBean gloss : glosses) {
				responseString.append("<glossary>");
				responseString.append("<id>" + gloss.getId() + "</id>");
				responseString.append("<category>" + gloss.getCategory()
						+ "</category>");
				responseString.append("<definition>" + gloss.getDefinition()
						+ "</definition>");
				responseString.append("</glossary>");
			}

			responseString.append("</glossaries>");

			response.getWriter().write(responseString.toString());
		} catch (Exception exe) {
			LOGGER.error("unable to get glossaries for term", exe);
			exe.printStackTrace();
		}
		return null;
	}

}