/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projekti;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author vehis
 */
@Controller
public class ProfileController {
    
    @Autowired
    private ProfileRepository profileRepository;
    
    @Autowired
    private TaitoRepository taitoRepository;
    
    @GetMapping("/profile/{merkkijono}")
    public String nayta(Model model, @PathVariable String merkkijono){
        
        if (profileRepository.findByMerkkijono(merkkijono)==null){
            return "etusivu";
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Profile profile = profileRepository.findByMerkkijono(merkkijono); 
        List<Taito> kaikki = profile.getTaidot();
        List<Taito> top3 = kaikki.stream().sorted(Comparator.comparing(Taito::getKehut).reversed()).limit(3).collect(Collectors.toList());
        for (Taito taito: top3){
            kaikki.remove(taito);
        }
        model.addAttribute("profile", profileRepository.findByMerkkijono(merkkijono));
        model.addAttribute("top3", top3);
        model.addAttribute("muut", kaikki);
        
        if (profileRepository.findByMerkkijono(merkkijono)==profileRepository.findByKayttajatunnus(username)){
            return "oma";
        }
        
        return "profile";
    }
    
   
    @PostMapping("profile/{merkkijono}/taito/{id}")
    public String kehu(RedirectAttributes ra, @PathVariable String merkkijono, @PathVariable Long id){
        Taito taito = taitoRepository.getOne(id);
        int kehut = taito.getKehut();
        List <Profile> kehujat = taito.getKehujat();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Profile profile = profileRepository.findByKayttajatunnus(username);
        if (kehujat.contains(profile)){
           ra.addFlashAttribute("taitoerror", "Olet jo kehunut tätä taitoa");
           return "redirect:/profile/" + merkkijono;
        }
        taito.setKehut(kehut+1);
            
        kehujat.add(profile);
        taito.setKehujat(kehujat);
        taitoRepository.save(taito);
        ra.addFlashAttribute("taitosuccess", "Kehu lisätty."); 
        return "redirect:/profile/" + merkkijono;
    }
    
    @Transactional
    @PostMapping("profile/{merkkijono}/taito/{id}/poista")
    public String poistaTaito(RedirectAttributes ra, @PathVariable String merkkijono, @PathVariable Long id){
        Taito taito = taitoRepository.getOne(id);
        Profile profile = profileRepository.findByMerkkijono(merkkijono);
        List<Taito> taidot = profile.getTaidot();
        taidot.remove(taito);
        profile.setTaidot(taidot);
        profileRepository.save(profile);
        taitoRepository.delete(taito);
        ra.addFlashAttribute("poistettusuccess", "Taito poistettu.");
        return "redirect:/profile/" + merkkijono;
    }
    
    @PostMapping("profile/{merkkijono}/taito/lisaa")
    public String lisaaTaito(RedirectAttributes ra, @PathVariable String merkkijono, @RequestParam String nimi){
        if (nimi == null || nimi.trim().isEmpty()) {
            ra.addFlashAttribute("nimierror", "Taitoa ei lisätty. Kirjoita taidolle nimi.");
            return "redirect:/profile/" + merkkijono;
        }
        Profile profile = profileRepository.findByMerkkijono(merkkijono);
        Taito taito = new Taito();
        taito.setNimi(nimi);
        taito.setKehut(0);
        taitoRepository.save(taito);
        List<Taito> taidot = profile.getTaidot();
        taidot.add(taito);
        profile.setTaidot(taidot);
        profileRepository.save(profile);
        ra.addFlashAttribute("lisattysuccess", "Taito lisätty.");
        return "redirect:/profile/" + merkkijono;
    }
    
    @Transactional
    @PostMapping("/profile/{merkkijono}/pyyda")
    public String pyyda(RedirectAttributes ra, @PathVariable String merkkijono){
        
        Profile profile = profileRepository.findByMerkkijono(merkkijono);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Profile pyytaja = profileRepository.findByKayttajatunnus(username);
        
        List<Profile> hyvaksyttavat = profile.getHyvaksyttavat();
        
        List<Profile> yhteydet = profile.getYhteydet();
        
        List<Profile> omathyvaksyttavat = pyytaja.getHyvaksyttavat();
        
        List<Profile> omatyhteydet = pyytaja.getYhteydet();
        
        if(omathyvaksyttavat.contains(profile)) {
            omathyvaksyttavat.remove(profile);
            omatyhteydet.add(profile);
            yhteydet.add(pyytaja);
            pyytaja.setHyvaksyttavat(omathyvaksyttavat);
            pyytaja.setYhteydet(omatyhteydet);
            profile.setYhteydet(yhteydet);
            profileRepository.save(pyytaja);
            profileRepository.save(profile);
            ra.addFlashAttribute("yhteyslisatty", "Yhteys lisätty. Pyytämäsi henkilö oli jo pyytänyt sinua yhteydeksi.");
            return "redirect:/profile/" + merkkijono;
        }
        
        if(hyvaksyttavat.contains(pyytaja) || yhteydet.contains(pyytaja)) {
            ra.addFlashAttribute("yhteyserror", "Yhteyspyyntöä ei lähetetty. Olet jo lähettänyt henkilölle yhteyspyynnön ja/tai hän on jo yhteyksissäsi.");
            return "redirect:/profile/" + merkkijono;
        }
        hyvaksyttavat.add(pyytaja);
        profile.setHyvaksyttavat(hyvaksyttavat);
        profileRepository.save(profile);
        
        ra.addFlashAttribute("yhteyssuccess", "Yhteyspyyntö lähetetty.");
        return "redirect:/profile/" + merkkijono;
        
    }
    
    @Transactional
    @PostMapping("/profile/{merkkijono}/hyvaksyttavat/{id}/hyvaksy")
    public String hyvaksyYhteys(RedirectAttributes ra, @PathVariable String merkkijono, @PathVariable Long id){
        
        Profile profile = profileRepository.findByMerkkijono(merkkijono);
        Profile hyvaksyttava = profileRepository.getOne(id);
        
        List<Profile> hyvaksyttavat = profile.getHyvaksyttavat();
        List<Profile> omatyhteydet = profile.getYhteydet();
        List<Profile> toisenyhteydet = hyvaksyttava.getYhteydet();
        hyvaksyttavat.remove(hyvaksyttava);
        omatyhteydet.add(hyvaksyttava);
        toisenyhteydet.add(profile);
        profile.setHyvaksyttavat(hyvaksyttavat);
        profile.setYhteydet(omatyhteydet);
        hyvaksyttava.setYhteydet(toisenyhteydet);
        profileRepository.save(hyvaksyttava);
        profileRepository.save(profile);
        
        ra.addFlashAttribute("hyvaksyttysuccess", "Yhteys hyväksytty.");
        return "redirect:/profile/" + merkkijono;
    }
    
    @Transactional
    @PostMapping("/profile/{merkkijono}/hyvaksyttavat/{id}/hylkaa")
    public String hylkaaYhteys(RedirectAttributes ra, @PathVariable String merkkijono, @PathVariable Long id){
        Profile profile = profileRepository.findByMerkkijono(merkkijono);
        Profile hyvaksyttava = profileRepository.getOne(id);
        
        List<Profile> hyvaksyttavat = profile.getHyvaksyttavat();
        
        hyvaksyttavat.remove(hyvaksyttava);
        profile.setHyvaksyttavat(hyvaksyttavat);
        
        profileRepository.save(hyvaksyttava);
        profileRepository.save(profile);
        
        ra.addFlashAttribute("hylattysuccess", "Yhteys hylätty.");
        return "redirect:/profile/" + merkkijono;
    }
    
    @Transactional
    @PostMapping("/profile/{merkkijono}/yhteydet/{id}/poista")
    public String poistaYhteys(RedirectAttributes ra, @PathVariable String merkkijono, @PathVariable Long id){
        
        Profile profile = profileRepository.findByMerkkijono(merkkijono);
        Profile yhteys = profileRepository.getOne(id);
        
        List<Profile> omatyhteydet = profile.getYhteydet();
        List<Profile> toisenyhteydet = yhteys.getYhteydet();
        
        omatyhteydet.remove(yhteys);
        toisenyhteydet.remove(profile);
        
        profile.setYhteydet(omatyhteydet);
        yhteys.setYhteydet(toisenyhteydet);
        
        profileRepository.save(profile);
        profileRepository.save(yhteys);
        
        ra.addFlashAttribute("poistosuccess", "Yhteys poistettu.");
        return "redirect:/profile/" + merkkijono;
        
    }
    
    @PostMapping("/profile/{merkkijono}/lisaakuva")
    public String lisaaKuva(RedirectAttributes ra, @ PathVariable String merkkijono, @RequestParam("file") MultipartFile file) throws IOException {
        
        Profile profile = profileRepository.findByMerkkijono(merkkijono);
        
        if(file.getContentType().equals("image/gif") || file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/png") || file.getContentType().equals("image/jpg")){
            byte[] profiilikuva = file.getBytes();
            profile.setProfiilikuva(profiilikuva);
            profileRepository.save(profile);
            ra.addFlashAttribute("kuvasuccess", "Profiilikuva lisätty.");
            return "redirect:/profile/" + merkkijono;
        }
        ra.addFlashAttribute("kuvaerror", "Kuvaa ei lisätty. Kuvan tiedostotyypin pitää olla gif, jpeg, jpg tai png.");
        return "redirect:/profile/" + merkkijono;
    }
    
    @GetMapping(path = "/profile/{merkkijono}/kuva", produces = {"image/gif", "image/jpeg", "image/png", "image/jpg"})
    @ResponseBody
    public byte[] profiilikuva(@PathVariable String merkkijono) throws IOException {
        
        Profile profile = profileRepository.findByMerkkijono(merkkijono);
        
        return profile.getProfiilikuva();
    }
    
    @GetMapping("/oma")
    public String oma(Model model){
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        Profile profile = profileRepository.findByKayttajatunnus(username);
        model.addAttribute("profile", profileRepository.findByKayttajatunnus(username));
        
        List<Taito> kaikki = profile.getTaidot();
        List<Taito> top3 = kaikki.stream().sorted(Comparator.comparing(Taito::getKehut).reversed()).limit(3).collect(Collectors.toList());
        for (Taito taito: top3){
            kaikki.remove(taito);
        }
        
        model.addAttribute("top3", top3);
        model.addAttribute("muut", kaikki);
        
        return "oma";
    }
    
    
    
    
    
    
}
