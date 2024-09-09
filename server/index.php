<?php
header('Content-Type: application/json');

// Percorso del file JSON
$file_path = 'stadi.json';

// Funzione per verificare se il file è scrivibile
function is_writable_file($file_path) {
    $file = @fopen($file_path, 'a');
    if ($file === false) {
        return false;
    }
    fclose($file);
    return true;
}

// Verifica se il file è scrivibile
if (!is_writable_file($file_path)) {
    echo json_encode(['error' => 'Il file non è scrivibile']);
    exit;
}

// Leggi il contenuto del file JSON
$json = file_get_contents($file_path);

// Decodifica il contenuto JSON
$stadi = json_decode($json, true);

// Verifica il metodo della richiesta
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Restituisci il contenuto del file JSON
    echo json_encode($stadi);
} elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Ottieni il corpo della richiesta POST
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    // Ottieni i dati dal corpo della richiesta JSON
    $nome = $data['nome'] ?? null;
    $latitudine = $data['latitudine'] ?? null;
    $longitudine = $data['longitudine'] ?? null;

    // Verifica che tutti i campi siano stati forniti
    if ($nome && $latitudine && $longitudine) {
        // Crea un nuovo stadio
        $nuovoStadio = [
            'nome' => $nome,
            'latitudine' => (float)$latitudine,
            'longitudine' => (float)$longitudine
        ];

        // Aggiungi il nuovo stadio all'array esistente
        $stadi[] = $nuovoStadio;

        // Codifica l'array aggiornato in JSON
        $json = json_encode($stadi, JSON_PRETTY_PRINT);

        // Scrivi il nuovo contenuto nel file JSON
        file_put_contents($file_path, $json);

        echo json_encode(['success' => 'Stadio aggiunto con successo']);
    } else {
        echo json_encode(['error' => 'Dati mancanti']);
    }
} elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
    // Ottieni il nome dello stadio da eliminare dall'URL
    $nome = $_GET['nome'] ?? null;

    // Verifica che il nome sia stato fornito
    if ($nome) {
        // Cerca e rimuovi lo stadio dall'array
        $stadi = array_filter($stadi, function($stadio) use ($nome) {
            return $stadio['nome'] !== $nome;
        });

        // Codifica l'array aggiornato in JSON
        $json = json_encode(array_values($stadi), JSON_PRETTY_PRINT);

        // Scrivi il nuovo contenuto nel file JSON
        file_put_contents($file_path, $json);

        echo json_encode(['success' => 'Stadio eliminato con successo']);
    } else {
        echo json_encode(['error' => 'Nome dello stadio mancante']);
    }
} else {
    echo json_encode(['error' => 'Metodo non supportato']);
}
?>