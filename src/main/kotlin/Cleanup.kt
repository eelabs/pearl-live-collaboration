import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class Cleanup(private val project: Project) : Disposable {
    override fun dispose() {
        Registration.get(project)?.dispose()
    }

}