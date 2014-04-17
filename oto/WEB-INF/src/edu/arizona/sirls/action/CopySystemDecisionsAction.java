package edu.arizona.sirls.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import edu.arizona.sirls.db.CharacterDBAccess;
import edu.arizona.sirls.form.GeneralForm;
import edu.arizona.sirls.util.Forwardable;

public class CopySystemDecisionsAction extends ParserAction {
	private static final Logger LOGGER = Logger
			.getLogger(GetUserReportAction.class);

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		if (checkSessionValidity(request)) {
			try {
				GeneralForm gform = (GeneralForm) form;
				String responseText = "";
				String dataset = gform.getValue();
				CharacterDBAccess cdba = new CharacterDBAccess();
				int numCopied = cdba.copySystemDecisions(dataset);
				if (numCopied > -1) {
					responseText = Integer.toString(numCopied) + " system decisions copied. ";
				} else {
					responseText = "Faild in copying system decisions. Please try again later. ";
				}

				response.setContentType("text/xml");
				response.getWriter().write(
						"<response>" + responseText + "</response>");
			} catch (Exception exe) {
				exe.printStackTrace();
				LOGGER.error("Error in copying system decisions: ", exe);
				request.setAttribute(Forwardable.ERROR, exe.getCause());
				response.setContentType("text/xml");
				response.getWriter().write(
						"<response>Problem encountered in copying system decisions: "
								+ exe.getMessage() + "</response>");
			}
			return null;
		} else {
			return null;
		}
	}

}
