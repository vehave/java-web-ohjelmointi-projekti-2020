
package projekti;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Postaus extends AbstractPersistable<Long> {
    
    @ManyToOne
    private Profile lahettaja;
    
    private LocalDateTime lahetysaika;
    
    private String sisalto;
    
    private Integer tykkaykset;
    
    private Integer kommentit;
    
    @ManyToMany
    private List<Profile> tykkaajat = new ArrayList();
    
    @OneToMany
    private List<Kommentti> kommentteja = new ArrayList();
    
    
}
