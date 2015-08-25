package org.openmrs.module.uicommons.message;

import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.messagesource.PresentationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Integrates with Angular Translate, simply initialize as follows:
 * $translateProvider.useUrlLoader('/' + OPENMRS_CONTEXT_PATH + '/module/uicommons/messages/messages.json')
 */
@Controller
public class MessagesController {

    @Autowired
    private MessageSourceService messageSourceService;

    private static Map<Locale, Map<String,String>> messages;

    private static Set<String> codes;

    private static String eTag;

    @RequestMapping(value = "/module/uicommons/messages/messages.json", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String,String>> getMessages(@RequestParam("lang") String lang, WebRequest webRequest) {

        // TODO zip?
        // TODO how do we do a refresh on context refreshed (or are controllers refreshed by default?) would this include static methods

        Locale locale = new Locale(lang);

        // create the translation map if we haven't already
        if (messages == null) {
            messages = new HashMap<Locale, Map<String, String>>();
            eTag = UUID.randomUUID().toString();
            initializeKeySet();
        }

        // see if we have this locale
        if (!messages.containsKey(locale)) {
            generateMessagesForLocale(locale);
        }

        String eTagFromClient = webRequest.getHeader("If-None-Match");

        // see if this client already has the right version cached, if so send back not modified
        if (eTagFromClient != null && eTagFromClient.contains(eTag)) {
            return new ResponseEntity<Map<String, String>>(new HashMap<String, String>(), HttpStatus.NOT_MODIFIED);
        }
        // otherwise set eTag and return the codes requested
        else {
            HttpHeaders headers = new HttpHeaders();
            headers.setETag(eTag);
            return new ResponseEntity<Map<String, String>>(messages.get(locale), headers, HttpStatus.OK);
        }

    }

    private void initializeKeySet() {
        codes = new HashSet<String>();
        for (PresentationMessage message : messageSourceService.getPresentations()) {
            codes.add(message.getCode());
        }
    }

    private void generateMessagesForLocale(Locale locale) {
        Map<String,String> m = new HashMap<String, String>();
        for (String code : codes) {
            m.put(code, messageSourceService.getMessage(code, null, locale));
        }
        messages.put(locale, m);
    }

}
