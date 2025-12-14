@SpringBootApplication(
scanBasePackages = "com.sc",
excludeFilters = @ComponentScan.Filter(
type = FilterType.ASSIGNABLE_TYPE,
classes = com.sc.apiframework.payload.config.WebConfig.class
)
)

public class BookTransfers {