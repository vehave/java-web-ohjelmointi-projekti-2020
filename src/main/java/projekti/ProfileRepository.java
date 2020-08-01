
package projekti;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Profile findByKayttajatunnus(String username);

    Profile findByMerkkijono(String merkkijono);

    Profile findByNimi(String nimi);
    
}
