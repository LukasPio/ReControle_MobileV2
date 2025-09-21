package com.lucas.recontrole.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.lucas.recontrole.Status
import com.lucas.recontrole.db.AppDatabase
import com.lucas.recontrole.dtos.OccurrenceDTO
import com.lucas.recontrole.model.OccurrenceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.isEmpty

// FUNÇÃO DE DELETAR CORRIGIDA
fun deleteOccurrence(
    context: Context,
    occurrenceId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    Log.d("DeleteOccurrence", "Iniciando deleção do item: $occurrenceId")

    val userId = Firebase.auth.currentUser?.uid
    if (userId == null) {
        Log.e("DeleteOccurrence", "Usuário não autenticado")
        onError("Usuário não autenticado.")
        return
    }

    if (occurrenceId.isEmpty()) {
        Log.e("DeleteOccurrence", "ID da ocorrência está vazio")
        onError("ID da ocorrência inválido.")
        return
    }

    try {
        // Primeiro, tentar deletar do Firebase
        val reportRef = FirebaseDatabase.getInstance()
            .getReference("reports")
            .child(occurrenceId)
            .child("content")

        Log.d("DeleteOccurrence", "Tentando deletar do Firebase: reports/$occurrenceId/content")

        // Verificar se o item existe antes de tentar deletar
        reportRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Log.w("DeleteOccurrence", "Item não existe no Firebase: $occurrenceId")
                onError("Item não encontrado.")
                return@addOnSuccessListener
            }

            // Verificar se o usuário é o autor
            val autor = snapshot.child("autor").getValue(String::class.java)
            if (autor != userId) {
                Log.w("DeleteOccurrence", "Usuário $userId tentando deletar item de $autor")
                onError("Você só pode deletar suas próprias ocorrências.")
                return@addOnSuccessListener
            }

            // Delete lógico: adiciona campo 'deleted' com timestamp
            val deleteData = mapOf(
                "deleted" to true,
                "deletedAt" to System.currentTimeMillis(),
                "deletedBy" to userId
            )

            Log.d("DeleteOccurrence", "Aplicando delete lógico...")
            reportRef.updateChildren(deleteData)
                .addOnSuccessListener {
                    Log.d("DeleteOccurrence", "Delete no Firebase bem-sucedido")

                    // Remove do cache local
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val dao = db.occurrenceDao()

                            // Verificar se existe no banco local antes de deletar
                            val existingItem = dao.findById(occurrenceId)
                            if (existingItem != null) {
                                dao.deleteById(occurrenceId)
                                Log.d("DeleteOccurrence", "Item removido do cache local")
                            } else {
                                Log.w("DeleteOccurrence", "Item não estava no cache local")
                            }

                            withContext(Dispatchers.Main) {
                                Log.d("DeleteOccurrence", "Deleção concluída com sucesso")
                                onSuccess()
                            }

                        } catch (e: Exception) {
                            Log.e("DeleteOccurrence", "Erro ao remover do cache local: ${e.message}")
                            // Mesmo com erro no cache, se deletou do Firebase é sucesso
                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DeleteOccurrence", "Erro ao deletar do Firebase: ${e.message}")
                    onError("Erro ao deletar do servidor: ${e.message}")
                }

        }.addOnFailureListener { e ->
            Log.e("DeleteOccurrence", "Erro ao verificar existência do item: ${e.message}")
            onError("Erro ao acessar o servidor: ${e.message}")
        }

    } catch (e: Exception) {
        Log.e("DeleteOccurrence", "Erro fatal na função de deletar: ${e.message}")
        onError("Erro inesperado: ${e.message}")
    }
}

fun saveOccurrence(
    context: Context,
    occurrenceDTO: OccurrenceDTO,
    onSuccess: () -> Unit,
    onError: ((String) -> Unit)? = null
) {
    val userId = Firebase.auth.currentUser?.uid
    if (userId == null) {
        Log.e("Firebase", "Usuário não autenticado.")
        onError?.invoke("Usuário não autenticado.")
        return
    }

    try {
        // Validar dados antes de salvar
        if (occurrenceDTO.description.isEmpty() ||
            occurrenceDTO.local.isEmpty() ||
            occurrenceDTO.category.isEmpty()) {
            onError?.invoke("Dados obrigatórios não preenchidos")
            return
        }

        // Validar Base64 da imagem
        if (occurrenceDTO.imgBase64.isNotEmpty() && base64ToBitmap(occurrenceDTO.imgBase64) == null) {
            onError?.invoke("Imagem inválida")
            return
        }

        val reportsRef = FirebaseDatabase.getInstance().getReference("reports")
        val occurrenceData = mapOf(
            "autor" to userId,
            "text" to occurrenceDTO.description,
            "status" to "red",
            "img_url" to occurrenceDTO.imgBase64,
            "local" to occurrenceDTO.local,
            "category" to occurrenceDTO.category,
            "timestamp" to System.currentTimeMillis()
        )

        val newRef = reportsRef.push()
        val newId = newRef.key ?: ""

        newRef.child("content").setValue(occurrenceData)
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.occurrenceDao()
                        dao.insertAll(
                            listOf(
                                OccurrenceEntity(
                                    id = newId,
                                    description = occurrenceDTO.description,
                                    imgBase64 = occurrenceDTO.imgBase64,
                                    local = occurrenceDTO.local,
                                    author = userId,
                                    status = "red",
                                    category = occurrenceDTO.category
                                )
                            )
                        )
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } catch (e: Exception) {
                        Log.e("Database", "Erro ao salvar localmente: ${e.message}")
                        withContext(Dispatchers.Main) {
                            onSuccess() // Salvo no Firebase, mas erro local não é crítico
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Erro ao salvar ocorrência: ${e.message}")
                onError?.invoke("Erro ao salvar: ${e.message}")
            }

    } catch (e: Exception) {
        Log.e("SaveOccurrence", "Erro fatal ao salvar: ${e.message}")
        onError?.invoke("Erro inesperado: ${e.message}")
    }
}


fun getOccurrences(
    context: Context,
    forceRefresh: Boolean = false,
    onResult: (List<OccurrenceDTO>) -> Unit
) {
    val db = AppDatabase.getDatabase(context)
    val dao = db.occurrenceDao()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Se não forçar refresh, tenta carregar do cache primeiro
            if (!forceRefresh) {
                val cached = dao.getAll()
                if (cached.isNotEmpty()) {
                    Log.d("GetOccurrences", "Carregando ${cached.size} items do cache")
                    val mapped = cached.mapNotNull { entity ->
                        try {
                            entity.toDTO()
                        } catch (e: Exception) {
                            Log.e("DataMapping", "Erro ao converter entidade: ${e.message}")
                            null
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onResult(mapped)
                    }
                    return@launch
                }
            }

            val userId = Firebase.auth.currentUser?.uid
            if (userId == null) {
                Log.e("Auth", "Usuário não autenticado")
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
                return@launch
            }

            Log.d("GetOccurrences", "Buscando dados do Firebase para userId: $userId")
            val reportsRef = FirebaseDatabase.getInstance().getReference("reports")

            reportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("Firebase", "Snapshot recebido com ${snapshot.childrenCount} children")

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val occurrences = mutableListOf<OccurrenceDTO>()
                            val entities = mutableListOf<OccurrenceEntity>()

                            for (child in snapshot.children) {
                                try {
                                    val content = child.child("content")
                                    Log.d("Firebase", "Processando child: ${child.key}")

                                    // Pula itens deletados
                                    val isDeleted = content.child("deleted").getValue(Boolean::class.java) ?: false
                                    if (isDeleted) {
                                        Log.d("Firebase", "Item ${child.key} está deletado, pulando")
                                        continue
                                    }

                                    val id = child.key ?: ""
                                    val description = content.child("text").getValue(String::class.java) ?: ""
                                    val category = content.child("category").getValue(String::class.java) ?: ""
                                    val imgBase64 = content.child("img_url").getValue(String::class.java) ?: ""
                                    var local = content.child("local").getValue(String::class.java) ?: ""
                                    val author = content.child("autor").getValue(String::class.java) ?: ""
                                    val statusString = content.child("status").getValue(String::class.java) ?: "red"

                                    if (local.isEmpty()) {
                                        local = child.child("selected_obj/sel_lab_id").getValue(String::class.java).toString()
                                    }

                                    Log.d("Firebase", "Dados do item $id: desc=$description, local=$local, category=$category")

                                    // Verificar se é um item válido
                                    if (id.isEmpty() || description.isEmpty()) {
                                        Log.w("Firebase", "Item $id tem dados inválidos, pulando")
                                        continue
                                    }

                                    val status = when (statusString) {
                                        "yellow" -> Status.ON_PROGRESS
                                        "green" -> Status.FINISHED
                                        else -> Status.PENDENT
                                    }

                                    val dto = OccurrenceDTO(
                                        id = id,
                                        description = description,
                                        category = category,
                                        imgBase64 = imgBase64,
                                        local = local,
                                        author = author,
                                        status = status
                                    )

                                    val entity = OccurrenceEntity(
                                        id = id,
                                        description = description,
                                        status = statusString,
                                        imgBase64 = imgBase64,
                                        local = local,
                                        author = author,
                                        category = category
                                    )

                                    occurrences.add(dto)
                                    entities.add(entity)

                                    Log.d("Firebase", "Item $id adicionado com sucesso")

                                } catch (e: Exception) {
                                    Log.e("Firebase", "Erro ao processar item ${child.key}: ${e.message}")
                                    continue
                                }
                            }

                            Log.d("Firebase", "Total de ${occurrences.size} ocorrências processadas")

                            // Salvar no cache
                            if (entities.isNotEmpty()) {
                                try {
                                    dao.clearAll()
                                    dao.insertAll(entities)
                                    Log.d("Database", "${entities.size} items salvos no cache")
                                } catch (e: Exception) {
                                    Log.e("Database", "Erro ao salvar no cache: ${e.message}")
                                }
                            }

                            withContext(Dispatchers.Main) {
                                Log.d("GetOccurrences", "Retornando ${occurrences.size} ocorrências para a UI")
                                onResult(occurrences)
                            }

                        } catch (e: Exception) {
                            Log.e("Firebase", "Erro geral no processamento: ${e.message}")
                            withContext(Dispatchers.Main) {
                                onResult(emptyList())
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro ao buscar dados: ${error.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        onResult(emptyList())
                    }
                }
            })

        } catch (e: Exception) {
            Log.e("GetOccurrences", "Erro fatal: ${e.message}")
            withContext(Dispatchers.Main) {
                onResult(emptyList())
            }
        }
    }
}


fun OccurrenceEntity.toDTO() = OccurrenceDTO(
    id = id,
    description = description,
    imgBase64 = imgBase64,
    local = local,
    author = author,
    status = when (status) {
        "red" -> Status.PENDENT
        "yellow" -> Status.ON_PROGRESS
        "green" -> Status.FINISHED
        else -> Status.PENDENT
    }
)

fun OccurrenceDTO.toEntity() = OccurrenceEntity(
    id = id,
    description = description,
    status = when (status) {
        Status.PENDENT -> "red"
        Status.ON_PROGRESS -> "yellow"
        Status.FINISHED -> "green"
    },
    imgBase64 = imgBase64,
    local = local,
    author = author,
    category = category
)

fun base64ToBitmap(base64String: String): Bitmap? {
    return try {
        if (base64String.isEmpty()) return null

        // Limpar o base64 de possíveis prefixos
        val cleanBase64 = base64String.replace("data:image/[^;]*;base64,".toRegex(), "")

        val decodeBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodeBytes, 0, decodeBytes.size)

        if (bitmap == null) {
            Log.e("ImageDecode", "Bitmap decodificado é null para: ${base64String.take(50)}...")
        }

        bitmap
    } catch (e: IllegalArgumentException) {
        Log.e("ImageDecode", "Base64 inválido: ${e.message}")
        null
    } catch (e: Exception) {
        Log.e("ImageDecode", "Erro ao decodificar imagem: ${e.message}")
        null
    }
}
