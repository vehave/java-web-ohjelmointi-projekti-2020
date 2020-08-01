
package projekti;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author vehis
 */
@Controller
public class HomeController {
    
    @Autowired
    private ProfileRepository profileRepository;
    
    @Autowired
    private PostausRepository postausRepository;
    
    @Autowired
    private KommenttiRepository kommenttiRepository;
    
    @Autowired
    PasswordEncoder passwordEncoder;
    
    @PostMapping("etusivu/rekisteroidy") 
    public String rekisteroidy(RedirectAttributes ra, @RequestParam String kayttajatunnus, @RequestParam String salasana, @RequestParam String nimi, @RequestParam String merkkijono) {
        if (kayttajatunnus == null || kayttajatunnus.trim().isEmpty() || salasana == null || salasana.trim().isEmpty() || nimi == null || nimi.trim().isEmpty() || merkkijono == null || merkkijono.trim().isEmpty()) {
            ra.addFlashAttribute("emptyerror", "Rekisteröityminen epäonnistui. Täytäthän kaikki kentät.");
            return "redirect:/etusivu/sivu/1";
        }
        
        if (profileRepository.findByKayttajatunnus(kayttajatunnus)!=null || profileRepository.findByNimi(nimi)!=null || profileRepository.findByMerkkijono(merkkijono)!=null){
            ra.addFlashAttribute("error", "Rekisteröityminen epäonnistui. Käyttäjätunnus, nimi ja/tai merkkijono on jo käytössä.");
            return "redirect:/etusivu/sivu/1";
        }
        
        Profile profile = new Profile();
        profile.setKayttajatunnus(kayttajatunnus);
        profile.setSalasana(passwordEncoder.encode(salasana));
        profile.setNimi(nimi);
        profile.setMerkkijono(merkkijono);
        
        profileRepository.save(profile);
        ra.addFlashAttribute("success", "Rekisteröityminen onnistui.");
        return "redirect:/etusivu/sivu/1";
    }
    
    @GetMapping("/etusivu/sivu/{sivu}")
    public String list(Model model, @PathVariable Integer sivu) {
        List <Postaus> postaukset = postausRepository.findAll();
        int postauksia = postaukset.size();
        ArrayList<Integer> sivut = new ArrayList();
        int a = 1;
        while(true){
            sivut.add(a);
            postauksia = postauksia-25;
            if(postauksia>0){
                a++;
            }else{
                break;
            }
        }
        
        Pageable pageable = PageRequest.of(sivu-1, 25, Sort.by("lahetysaika").descending());
        model.addAttribute("postaukset", postausRepository.findAll(pageable));
        model.addAttribute("sivut", sivut);
        return "etusivu";
    }
    
    @PostMapping("etusivu/postaa")
    public String postaa(RedirectAttributes ra, @RequestParam String sisalto){
        if (sisalto == null || sisalto.trim().isEmpty()) {
            ra.addFlashAttribute("postauserror", "Postausta ei lähetetty. Kirjoita postaukselle sisältö.");
            return "redirect:/etusivu/sivu/1";
        }
        Postaus postaus = new Postaus();
        postaus.setSisalto(sisalto);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Profile profile = profileRepository.findByKayttajatunnus(username);
        postaus.setLahetysaika(LocalDateTime.now());
        postaus.setLahettaja(profile);
        postaus.setTykkaykset(0);
        postaus.setKommentit(0);
        postausRepository.save(postaus);
        ra.addFlashAttribute("postaussuccess", "Postaus lähetetty.");
        return "redirect:/etusivu/sivu/1";
    }
    
    @GetMapping("/etusivu/{id}/sivu/{sivu}")
    public String getOne(Model model, @PathVariable Long id, @PathVariable Integer sivu) {
        
        model.addAttribute("postaus", postausRepository.getOne(id));
        Postaus postaus = postausRepository.getOne(id);
        int i=0;
        List <Kommentti> kommentit = postaus.getKommentteja();
        int kommentteja = kommentit.size();
        ArrayList<Integer> sivut = new ArrayList();
        int a = 1;
        while(true){
            sivut.add(a);
            kommentteja = kommentteja-10;
            if(kommentteja>0){
                a++;
            }else{
                break;
            }
        }
        while (i<sivu){
            
            List <Kommentti> lista = kommentit.stream().sorted(Comparator.comparing(Kommentti::getLahetysaika).reversed()).limit(10).collect(Collectors.toList());
            for (Kommentti kommentti : lista){
                kommentit.remove(kommentti);
            }
            model.addAttribute("page", lista);
            i++;
            
        }
        model.addAttribute("sivut", sivut);
        
        return "postaus";
    }
    
    
    @PostMapping("etusivu/{id}/tykkaa")
    public String tykkaa(RedirectAttributes ra, @PathVariable Long id){
        Postaus postaus = postausRepository.getOne(id);
        int tykkaykset = postaus.getTykkaykset();
        List <Profile> tykkaajat = postaus.getTykkaajat();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Profile profile = profileRepository.findByKayttajatunnus(username);
        if (!tykkaajat.contains(profile)){
            postaus.setTykkaykset(tykkaykset+1);
            
            tykkaajat.add(profile);
            postaus.setTykkaajat(tykkaajat);
            postausRepository.save(postaus);
            ra.addFlashAttribute("tykatty", "Tykkäys lisätty.");
            return "redirect:/etusivu/sivu/1";
        }
        ra.addFlashAttribute("virhetykkays", "Olet jo tykännyt tästä postauksesta.");
        return "redirect:/etusivu/sivu/1";
        
    }
    
    @PostMapping("etusivu/{id}/postaus/tykkaa")
    public String tykkaa2(RedirectAttributes ra, @PathVariable Long id){
        Postaus postaus = postausRepository.getOne(id);
        int tykkaykset = postaus.getTykkaykset();
        List <Profile> tykkaajat = postaus.getTykkaajat();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Profile profile = profileRepository.findByKayttajatunnus(username);
        if (!tykkaajat.contains(profile)){
            postaus.setTykkaykset(tykkaykset+1);
            
            tykkaajat.add(profile);
            postaus.setTykkaajat(tykkaajat);
            postausRepository.save(postaus);
            ra.addFlashAttribute("tykatty", "Tykkäys lisätty.");
            return "redirect:/etusivu/" + id + "/sivu/1";
        }
        ra.addFlashAttribute("virhetykkays2", "Olet jo tykännyt tästä postauksesta.");
        return "redirect:/etusivu/" + id + "/sivu/1";
        
    }
    
    @PostMapping("/etusivu/{id}/kommentoi")
    public String kommentoi(RedirectAttributes ra, @PathVariable Long id, @RequestParam String sisalto){
        if (sisalto == null || sisalto.trim().isEmpty()) {
            ra.addFlashAttribute("kommenttierror", "Kommenttia ei lähetetty. Kirjoita kommentille sisältö.");
            return "redirect:/etusivu/" + id + "/sivu/1";
        }
        Postaus postaus = postausRepository.getOne(id);
        Kommentti kommentti = new Kommentti();
        kommentti.setLahetysaika(LocalDateTime.now());
        kommentti.setSisalto(sisalto);
        List<Kommentti> kommentteja = postaus.getKommentteja();
        kommentteja.add(kommentti);
        postaus.setKommentteja(kommentteja);
        postaus.setKommentit(postaus.getKommentit()+1);
        kommenttiRepository.save(kommentti);
        postausRepository.save(postaus);
        ra.addFlashAttribute("kommenttisuccess", "Kommentti lähetetty.");
        return "redirect:/etusivu/" + id + "/sivu/1";
        
    }
    
    @PostMapping("etusivu/hae") 
    public String hae(RedirectAttributes ra, Model model, @RequestParam String nimi){
        if (profileRepository.findByNimi(nimi)==null){
            ra.addFlashAttribute("fail", "Käyttäjää ei löytynyt.");
            return "redirect:/etusivu/sivu/1";
        }
        Profile profile = profileRepository.findByNimi(nimi);
        String merkkijono = profile.getMerkkijono();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        List<Taito> kaikki = profile.getTaidot();
        List<Taito> top3 = kaikki.stream().sorted(Comparator.comparing(Taito::getKehut).reversed()).limit(3).collect(Collectors.toList());
        for (Taito taito: top3){
            kaikki.remove(taito);
        }
        model.addAttribute("profile", profileRepository.findByNimi(nimi));
        model.addAttribute("top3", top3);
        model.addAttribute("muut", kaikki);
        if (profileRepository.findByNimi(nimi)==profileRepository.findByKayttajatunnus(username)){
            return "oma";
        }
        
        
        return "redirect:/profile/"+merkkijono;
    }
    

    
    
    
    
    
}
