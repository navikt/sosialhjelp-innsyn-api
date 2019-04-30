package no.nav.sbl.sosialhjelpinnsyn.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthcheckController {

    @GetMapping(value="/internal/isAlive")
    @ResponseBody
    public String isAlive(){
        return "ok";
    }

    @GetMapping(value = "/internal/isReady")
    @ResponseBody
    public String isReady() {
        return "ok";
    }
}
