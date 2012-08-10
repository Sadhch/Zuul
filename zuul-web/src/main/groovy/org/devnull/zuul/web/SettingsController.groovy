package org.devnull.zuul.web

import org.devnull.zuul.data.model.SettingsEntry
import org.devnull.zuul.data.model.SettingsGroup
import org.devnull.zuul.service.ZuulService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletResponse

import org.springframework.web.bind.annotation.*

@Controller
class SettingsController {

    @Autowired
    ZuulService zuulService

    /* -------- Create Settings Group Workflow ------ */

    /**
     * Start the workflow for creating a new settings group
     * @return
     */
    @RequestMapping(value = "/settings/new", method = RequestMethod.GET)
    String newSettingsGroupForm() {
        return "/settings/new"
    }

    /**
     * Begin a new settings group with no key/vales
     */
    @RequestMapping(value = "/settings/create/scratch")
    String createFromScratch(@RequestParam("name") String name, @RequestParam("environment") String env) {
        def settingsGroup = zuulService.createEmptySettingsGroup(name, env)
        return "redirect:/settings/${settingsGroup.name}#${settingsGroup.environment.name}"
    }

    /* -------- View/Render Actions ------ */

    /**
     * Render a java properties file usable in an application.
     */
    @RequestMapping(value = "/settings/{environment}/{name}.properties", method = RequestMethod.GET)
    void renderPropertiesByNameAndEnv(HttpServletResponse response, @PathVariable("name") String name, @PathVariable("environment") String env) {
        def properties = zuulService.findSettingsGroupByNameAndEnvironment(name, env) as Properties
        response.setContentType("text/plain")
        properties.store(response.outputStream, "Generated from Zuul  with parameters: name=${name}, environment=${env}")
    }

    /**
     * User interface for editing settings group
     */
    @RequestMapping(value = "/settings/{name}", method = RequestMethod.GET)
    ModelAndView show(@PathVariable("name") String name) {
        def environments = zuulService.listEnvironments()
        def groupsByEnv = [:]
        environments.each { env ->
            groupsByEnv[env] = zuulService.findSettingsGroupByNameAndEnvironment(name, env.name)
        }
        def model = [groupsByEnv: groupsByEnv, groupName: name, environments: environments]
        return new ModelAndView("/settings/show", model)
    }

    /**
     * View all of the settings groups as JSON
     */
    @RequestMapping(value = "/settings.json")
    @ResponseBody
    List<SettingsGroup> listJson() {
        return zuulService.listSettingsGroups()
    }

    /**
     * View a specific entry in JSON
     */
    @RequestMapping(value = "/settings/entry/{id}.json", method = RequestMethod.GET)
    @ResponseBody
    SettingsEntry showEntryJson(@PathVariable("id") Integer id) {
        return zuulService.findSettingsEntry(id)
    }

    /* -------- Entry Modification Actions ------ */

    /**
     * Create a new key/value entry for the settings group
     */
    @RequestMapping(value = "/settings/{environment}/{name}/entry.json", method = RequestMethod.POST)
    @ResponseBody
    String addEntryJson(HttpServletResponse response, @PathVariable("name") String name,
                               @PathVariable("environment") String env, @RequestBody SettingsEntry entry) {
        def group = zuulService.findSettingsGroupByNameAndEnvironment(name, env)
        group.addToEntries(entry)
        zuulService.save(group)
        return "redirect:/settings/entry/${entry.id}.json"
    }

    /**
     * Replace a key/value entry for a settings group with new values
     */
    @RequestMapping(value = "/settings/entry/{id}.json", method = RequestMethod.PUT)
    @ResponseBody
    SettingsEntry updateEntryJson(@PathVariable("id") Integer id, @RequestBody SettingsEntry formEntry) {
        def entry = zuulService.findSettingsEntry(id)
        entry.key = formEntry.key
        entry.value = formEntry.value
        return zuulService.save(entry)
    }

    /**
     * Delete a key/value entry for a settings group
     */
    @RequestMapping(value = "/settings/entry/{id}.json", method = RequestMethod.DELETE)
    @ResponseBody()
    String deleteEntryJson(@PathVariable("id") Integer id, HttpServletResponse response) {
        zuulService.deleteSettingsEntry(id)
        response.status = 204
        return ""
    }

    /**
     * Encrypt a value for a key/value entry
     */
    @RequestMapping(value = "/settings/entry/encrypt.json")
    @ResponseBody
    SettingsEntry encrypt(@RequestParam("id") Integer id) {
        return zuulService.encryptSettingsEntryValue(id)
    }

    /**
     * Encrypt a value for a key/value entry
     */
    @RequestMapping(value = "/settings/entry/decrypt.json")
    @ResponseBody
    SettingsEntry decrypt(@RequestParam("id") Integer id) {
        return zuulService.decryptSettingsEntryValue(id)
    }
}
