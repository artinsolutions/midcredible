import com.evolveum.midpoint.midcredible.framework.util.Comparator
import com.evolveum.midpoint.midcredible.framework.util.State
import com.evolveum.midpoint.midcredible.framework.util.structural.Attribute
import com.evolveum.midpoint.midcredible.framework.util.structural.Identity
import com.evolveum.midpoint.midcredible.framework.util.structural.Outcome

class SimpleComparator implements Comparator {


    @Override
    String query() {
        return null
    }

    @Override
    State compareIdentity(Identity oldIdentity, Identity newIdentity) {

     String oldUid = oldIdentity.getUid()
     String newUid = newIdentity.getUid()

        Integer id = oldUid <=> newUid

        if (id==0){

            return State.EQUAL
        }else if (id >0){

            return State.OLD_AFTER_NEW
        }else{

            return State.OLD_BEFORE_NEW
        }

    }

    @Override
    Identity compareData(Identity oldIdentity, Identity newIdentity) {
        Map<String, Attribute> old = oldIdentity.getAttrs()

        for(String attrName:old){


        }

    }
}