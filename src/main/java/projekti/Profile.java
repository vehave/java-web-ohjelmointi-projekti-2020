/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projekti;


import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile extends AbstractPersistable<Long> {
    
    
    
    private String kayttajatunnus;
    
    
    
    private String salasana;
    
    
    
    private String nimi;
    
    
    private String merkkijono;
    
    @Lob
    private byte[] profiilikuva;
    
    @ManyToMany
    private List<Profile> yhteydet=new ArrayList();;
    
    @ManyToMany
    private List<Profile> hyvaksyttavat=new ArrayList();;
    
    @OneToMany
    private List<Taito> taidot=new ArrayList();
}
