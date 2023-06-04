package merits.funskills.pureland.utils;

import lombok.Data;
import merits.funskills.pureland.model.PlayItem;
import merits.funskills.pureland.model.PlayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Library {

  private Map<PlayList, List<PlayItem>> library = new HashMap<>();
}
